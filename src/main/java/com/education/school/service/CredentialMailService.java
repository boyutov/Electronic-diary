package com.education.school.service;

import com.education.school.config.TenantContext;
import com.education.school.entity.User;
import com.education.school.repository.SchoolRepository;
import com.education.school.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Выдача временных паролей: генерирует пароль и отправляет его пользователю на почту
// Пароль нигде не хранится в открытом виде и не попадает ни в ответ API, ни к ИИ —
// его видит только владелец почтового ящика
@Service
public class CredentialMailService {

    private static final Logger log = LoggerFactory.getLogger(CredentialMailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Письма уходят по одному в фоне: импорт 500 учеников не ждёт SMTP-сервер
    private final ExecutorService mailExecutor = Executors.newSingleThreadExecutor();

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.public-url:http://localhost:8088}")
    private String publicUrl;

    public CredentialMailService(ObjectProvider<JavaMailSender> mailSender,
                                 PasswordGenerator passwordGenerator,
                                 PasswordEncoder passwordEncoder,
                                 SchoolRepository schoolRepository,
                                 UserRepository userRepository,
                                 NotificationService notificationService) {
        this.mailSender = mailSender;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public boolean isConfigured() {
        return mailHost != null && !mailHost.isBlank() && mailSender.getIfAvailable() != null;
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                "Почта не настроена (MAIL_HOST в .env) — пароли некуда отправить. Укажите пароль вручную или настройте почту.");
        }
    }

    // Генерирует временный пароль для нового пользователя: пароль хэшируется в user,
    // а письмо с ним уходит после коммита транзакции (если сохранение откатится — письма не будет)
    public void issueTemporaryPassword(User user) {
        requireConfigured();
        String temporaryPassword = passwordGenerator.generateTemporary();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        sendAfterCommit(user, temporaryPassword);
    }

    // Пароль для пользователя, которого создаёт кто-то другой:
    // не указан — генерируем и отправляем на почту; указан — его знает создатель, поэтому при входе нужно сменить
    public void setInitialPassword(User user, String password) {
        if (password == null || password.isBlank()) {
            issueTemporaryPassword(user);
        } else {
            user.setPassword(passwordEncoder.encode(password));
            user.setMustChangePassword(true);
        }
    }

    // Пароль меняет не сам владелец (админ в форме редактирования) — владелец обязан сменить его при входе
    public void setPasswordByOther(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        Long currentId = currentUserId();
        user.setMustChangePassword(currentId == null || !currentId.equals(user.getId()));
    }

    private void sendAfterCommit(User user, String temporaryPassword) {
        Long schoolId = TenantContext.getRequiredSchoolId();
        String schoolName = schoolRepository.findById(schoolId).map(s -> s.getName()).orElse("");
        Long initiatorId = currentUserId();
        String to = user.getMailAddress();
        String login = user.getEmail();
        boolean toParent = !to.equalsIgnoreCase(login); // у ребёнка нет своей почты — пишем родителю

        String text = (toParent ? """
                Здравствуйте!

                Для вашего ребёнка (%s %s) создан аккаунт в электронном дневнике.

                Вход: %s/login
                Школа: %s
                Логин ребёнка: %s
                Временный пароль: %s

                При первом входе система попросит придумать новый пароль — помогите ребёнку его запомнить.
                Никому не сообщайте пароль — даже учителю или администратору.
                """.formatted(user.getSecondName(), user.getFirstName(), publicUrl, schoolName, login, temporaryPassword)
                : """
                Здравствуйте, %s!

                Для вас создан аккаунт в электронном дневнике.

                Вход: %s/login
                Школа: %s
                Логин: %s
                Временный пароль: %s

                При первом входе система попросит придумать свой пароль.
                Никому не сообщайте пароль — даже учителю или администратору.
                """.formatted(user.getFirstName(), publicUrl, schoolName, login, temporaryPassword));

        afterCommit(() -> send(to, "Доступ к электронному дневнику — " + schoolName, text, schoolId, initiatorId));
    }

    // Письмо со ссылкой "Забыли пароль?" — пароль меняется только после перехода по ссылке
    public void sendPasswordResetLink(User user, String schoolName, String token) {
        requireConfigured();
        String link = publicUrl + "/reset-password?token=" + token;
        String to = user.getMailAddress();
        boolean toParent = !to.equalsIgnoreCase(user.getEmail());
        String text = """
                Здравствуйте!

                Кто-то запросил восстановление пароля %s в электронном дневнике (школа %s, логин %s).

                Чтобы задать новый пароль, перейдите по ссылке (действует 1 час):
                %s

                Если вы не запрашивали восстановление — просто проигнорируйте письмо, пароль останется прежним.
                """.formatted(toParent ? "вашего ребёнка (" + user.getSecondName() + " " + user.getFirstName() + ")" : "вашего аккаунта",
                              schoolName, user.getEmail(), link);
        afterCommit(() -> send(to, "Восстановление пароля — " + schoolName, text, null, null));
    }

    // Письмо уходит после коммита транзакции: если сохранение откатится — письма не будет
    private void afterCommit(Runnable send) {
        Runnable task = () -> mailExecutor.submit(send);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private void send(String to, String subject, String text, Long schoolId, Long initiatorId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.getObject().send(message);
        } catch (Exception e) {
            log.warn("Не удалось отправить письмо на {}: {}", to, e.getMessage());
            notifyInitiator(to, schoolId, initiatorId);
        }
    }

    // Сообщаем тому, кто создал аккаунт: пароль можно выслать повторно после исправления почты
    private void notifyInitiator(String email, Long schoolId, Long initiatorId) {
        if (initiatorId == null || schoolId == null) return;
        try {
            TenantContext.setSchoolId(schoolId); // фоновый поток — школу выставляем сами
            notificationService.send(initiatorId, "ACCOUNT",
                    "✉️ Письмо с паролем не доставлено",
                    "Не удалось отправить пароль на " + email + ". Проверьте адрес и отправьте пароль повторно.",
                    "/admin/student");
        } catch (Exception e) {
            log.warn("Не удалось создать уведомление о недоставленном письме: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        User user = userRepository.findByEmail(auth.getName());
        return user != null ? user.getId() : null;
    }

    @PreDestroy
    void shutdown() {
        mailExecutor.shutdown();
    }
}
