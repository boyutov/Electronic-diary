package com.education.school.service;

import com.education.school.controller.PollController;
import com.education.school.dto.*;
import com.education.school.entity.*;
import com.education.school.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceSecurityRegressionTest {
    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    private User authenticate(String role) {
        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.org");
        user.setFirstName("Имя");
        user.setSecondName("Фамилия");
        Role entity = new Role(); entity.setName(role); user.setRole(entity);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority(role))));
        return user;
    }

    @Test void removingCuratorshipClearsGroupOwner() { checkCuratorUpdate(false, false); }
    @Test void movingCuratorClearsOldGroupAndAssignsNewOne() { checkCuratorUpdate(true, false); }
    @Test void changingTeacherDoesNotClearAnotherCuratorsAssignment() { checkCuratorUpdate(false, true); }

    private void checkCuratorUpdate(boolean move, boolean replaced) {
        User user = authenticate("TEACHER");
        GroupEntity oldGroup = new GroupEntity(); oldGroup.setId(1);
        User owner = user;
        if (replaced) { owner = new User(); owner.setId(20L); }
        oldGroup.setCurator(owner);
        GroupEntity newGroup = new GroupEntity(); newGroup.setId(2);
        Teacher teacher = new Teacher(); teacher.setId(3); teacher.setUser(user);
        teacher.setGroup(oldGroup); teacher.setHasGroup(true);
        TeacherRepository teachers = mock(TeacherRepository.class);
        GroupRepository groups = mock(GroupRepository.class);
        when(teachers.findById(3)).thenReturn(Optional.of(teacher));
        when(teachers.save(teacher)).thenReturn(teacher);
        if (move) when(groups.findById(2)).thenReturn(Optional.of(newGroup));
        TeacherService service = new TeacherService(teachers, mock(UserRepository.class), mock(RoleRepository.class),
                groups, mock(DisciplineRepository.class), mock(ScheduleRepository.class),
                mock(CurrentSchoolService.class), mock(CredentialMailService.class));
        service.update(3, new TeacherRequest("Имя", "Фамилия", null, user.getEmail(), null,
                null, null, false, null, move, move ? 2 : null, null));
        assertSame(replaced ? owner : null, oldGroup.getCurator());
        assertSame(move ? newGroup : null, teacher.getGroup());
        assertEquals(move, teacher.getHasGroup());
        if (move) assertSame(user, newGroup.getCurator());
        if (!replaced) verify(groups).save(oldGroup);
        else verify(groups, never()).save(oldGroup);
    }

    private Poll poll(boolean withVotes) {
        Poll poll = new Poll(); poll.setId(1); poll.setTitle("Опрос"); poll.setActive(true);
        poll.setAllowedRoles("STUDENT");
        for (int id = 1; id <= 2; id++) {
            PollOption option = new PollOption(); option.setId(id); option.setPoll(poll);
            option.setOptionText(id == 1 ? "Да" : "Нет"); poll.getOptions().add(option);
        }
        if (withVotes) {
            PollOption option = poll.getOptions().iterator().next();
            PollVote vote = new PollVote(); vote.setPoll(poll); vote.setOption(option);
            option.getVotes().add(vote); poll.getVotes().add(vote);
        }
        return poll;
    }

    @Test void closingAndRenamingPollPreservesOptionIdsAndVotesDespiteReordering() {
        Poll poll = poll(true);
        Set<PollOption> options = new HashSet<>(poll.getOptions());
        Set<PollVote> votes = new HashSet<>(poll.getVotes());
        PollRepository polls = mock(PollRepository.class);
        when(polls.findById(1)).thenReturn(Optional.of(poll));
        when(polls.save(poll)).thenReturn(poll);
        PollService service = new PollService(polls, mock(UserRepository.class));
        service.update(1, new PollRequest("Новое название", "Описание", List.of("Нет", "Да"), false, "TEACHER"));
        assertEquals(options, poll.getOptions());
        assertEquals(votes, poll.getVotes());
        assertEquals(1, poll.getOptions().stream().mapToInt(o -> o.getVotes().size()).sum());
        assertEquals("Новое название", poll.getTitle()); assertFalse(poll.getActive());
    }

    @Test void changingOptionsAfterVotingIsRejectedBeforeAnyMutation() {
        Poll poll = poll(true);
        Set<PollOption> options = new HashSet<>(poll.getOptions());
        PollRepository polls = mock(PollRepository.class);
        when(polls.findById(1)).thenReturn(Optional.of(poll));
        PollService service = new PollService(polls, mock(UserRepository.class));
        assertThrows(IllegalArgumentException.class, () -> service.update(1,
                new PollRequest("Новое название", null, List.of("Другой ответ"), false, null)));
        assertEquals(options, poll.getOptions()); assertEquals("Опрос", poll.getTitle());
        assertTrue(poll.getActive()); assertEquals(1, poll.getVotes().size());
        verify(polls, never()).save(any());
    }

    @Test void optionsCanBeReplacedBeforeVoting() {
        Poll poll = poll(false);
        PollRepository polls = mock(PollRepository.class);
        when(polls.findById(1)).thenReturn(Optional.of(poll));
        new PollService(polls, mock(UserRepository.class)).update(1,
                new PollRequest("Опрос", null, List.of("Другой ответ"), true, null));
        assertEquals(1, poll.getOptions().size());
        PollOption option = poll.getOptions().iterator().next();
        assertEquals("Другой ответ", option.getOptionText()); assertSame(poll, option.getPoll());
    }

    @Test void duplicateOptionTextsAreNotMistakenForUnchangedOptions() {
        Poll poll = poll(true);
        PollRepository polls = mock(PollRepository.class);
        when(polls.findById(1)).thenReturn(Optional.of(poll));
        assertThrows(IllegalArgumentException.class, () -> new PollService(polls, mock(UserRepository.class))
                .update(1, new PollRequest("Опрос", null, List.of("Да", "Нет", "Нет"), true, null)));
    }

    @Test void studentCannotReadTeacherPollById() { checkPollRead("STUDENT", "TEACHER", true, false); }
    @Test void studentCannotReadClosedPollById() { checkPollRead("STUDENT", "STUDENT", false, false); }
    @Test void allowedRoleCanReadActivePoll() { checkPollRead("STUDENT", " teacher, student ", true, true); }
    @Test void blankRolesAllowActivePoll() { checkPollRead("PARENT", "", true, true); }
    @Test void adminCanReadClosedRestrictedPoll() { checkPollRead("ADMIN", "TEACHER", false, true); }
    @Test void directorCanReadClosedRestrictedPoll() { checkPollRead("DIRECTOR", "TEACHER", false, true); }

    private void checkPollRead(String role, String allowed, boolean active, boolean accessible) {
        User user = authenticate(role);
        Poll poll = poll(false); poll.setAllowedRoles(allowed); poll.setActive(active);
        PollRepository polls = mock(PollRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(polls.findById(1)).thenReturn(Optional.of(poll));
        when(users.findByEmail(user.getEmail())).thenReturn(user);
        PollService service = new PollService(polls, users);
        if (accessible) assertSame(poll, service.findById(1));
        else assertThrows(SecurityException.class, () -> service.findById(1));
    }

    @Configuration
    @EnableMethodSecurity
    static class PollSecurityConfig {
        @Bean PollService pollService() { return mock(PollService.class); }
        @Bean PollController pollController(PollService service) { return new PollController(service); }
    }

    @Test void generalPollListRequiresAdminOrDirectorAtControllerBoundary() {
        try (var context = new AnnotationConfigApplicationContext(PollSecurityConfig.class)) {
            PollController controller = context.getBean(PollController.class);
            PollService service = context.getBean(PollService.class);
            for (String role : List.of("STUDENT", "PARENT", "TEACHER")) {
                authenticate(role);
                assertThrows(AccessDeniedException.class, () -> controller.findAll("school"));
            }
            verifyNoInteractions(service);
            when(service.findAll()).thenReturn(List.of());
            for (String role : List.of("ADMIN", "DIRECTOR")) {
                authenticate(role); assertEquals(List.of(), controller.findAll("school"));
            }
            verify(service, times(2)).findAll();
        }
    }

    private ProfileService profileService(UserRepository users, PasswordEncoder encoder) {
        return new ProfileService(users, mock(GroupRepository.class), mock(TeacherRepository.class),
                mock(StudentRepository.class), mock(ParentRepository.class), encoder, mock(CurrentSchoolService.class));
    }

    @Test void profileEndpointRejectsPasswordWithoutSavingAnyProfileChanges() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        assertThrows(IllegalArgumentException.class, () -> profileService(users, encoder).updateProfile(
                new ProfileUpdateRequest("Имя", "Фамилия", null, "user@example.org", "attacker-password", null, null, null)));
        verifyNoInteractions(users, encoder);
    }

    @Test void ordinaryProfileUpdateStillWorksWithoutPassword() {
        User user = authenticate("ADMIN"); user.setPassword("old-hash");
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(user.getEmail())).thenReturn(user);
        when(users.save(user)).thenReturn(user);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserProfileDto result = profileService(users, encoder).updateProfile(new ProfileUpdateRequest(
                "Новое имя", "Фамилия", null, user.getEmail(), null, null, null, null));
        assertEquals("Новое имя", result.getFirstName()); assertEquals("old-hash", user.getPassword());
        verifyNoInteractions(encoder);
    }

    @Test void dedicatedPasswordEndpointRejectsWrongCurrentPassword() {
        User user = authenticate("ADMIN"); user.setPassword("old-hash");
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(user.getEmail())).thenReturn(user);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.matches("wrong", "old-hash")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> profileService(users, encoder)
                .changePassword(new PasswordChangeRequest("wrong", "new-password")));
        assertEquals("old-hash", user.getPassword()); verify(users, never()).save(any());
    }

    @Test void dedicatedPasswordEndpointChangesPasswordAndRevokesOldTokens() {
        User user = authenticate("ADMIN"); user.setPassword("old-hash"); user.setMustChangePassword(true);
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(user.getEmail())).thenReturn(user);
        when(users.save(user)).thenReturn(user);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.matches("old-password", "old-hash")).thenReturn(true);
        when(encoder.encode("new-password")).thenReturn("new-hash");
        assertSame(user, profileService(users, encoder).changePassword(new PasswordChangeRequest("old-password", "new-password")));
        assertEquals("new-hash", user.getPassword()); assertFalse(user.getMustChangePassword());
        assertNotNull(user.getPasswordChangedAt());
    }
}
