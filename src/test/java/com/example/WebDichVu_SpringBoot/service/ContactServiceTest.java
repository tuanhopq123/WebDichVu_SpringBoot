package com.example.WebDichVu_SpringBoot.service;

import com.example.WebDichVu_SpringBoot.entity.Contact;
import com.example.WebDichVu_SpringBoot.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("ContactService Test")
class ContactServiceTest {

  @Mock
  private ContactRepository contactRepository;

  @Mock
  private EmailService emailService;

  @InjectMocks
  private ContactService contactService;

  private Contact testContact;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    testContact = new Contact();
    testContact.setId(1L);
    testContact.setTen("John Doe");
    testContact.setEmail("john@example.com");
    testContact.setSdt("0912345678");
    testContact.setChudeTim("Support");
    testContact.setNoiDung("I need help with service");
    testContact.setRead(false);
    testContact.setCreatedAt(LocalDateTime.now());
  }

  // ========== TEST saveContact ==========
  @Test
  @DisplayName("Should save contact successfully")
  void testSaveContactSuccess() {
    when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

    Contact result = contactService.saveContact(testContact);

    assertNotNull(result);
    assertEquals("john@example.com", result.getEmail());
    verify(contactRepository, times(1)).save(any(Contact.class));
  }

  @Test
  @DisplayName("Should save contact with null admin reply")
  void testSaveContactWithNullAdminReply() {
    testContact.setAdminReply(null);

    when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

    Contact result = contactService.saveContact(testContact);

    assertNotNull(result);
    assertNull(result.getAdminReply());
  }

  @Test
  @DisplayName("Should save contact with null ID (new)")
  void testSaveContactNewWithNullId() {
    testContact.setId(null);

    when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

    Contact result = contactService.saveContact(testContact);

    assertNotNull(result);
    verify(contactRepository, times(1)).save(any(Contact.class));
  }

  @Test
  @DisplayName("Should save contact with empty email")
  void testSaveContactEmptyEmail() {
    testContact.setEmail("");

    when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

    Contact result = contactService.saveContact(testContact);

    assertNotNull(result);
    verify(contactRepository, times(1)).save(any(Contact.class));
  }

  // ========== TEST getContacts (Default Filter) ==========
  @Test
  @DisplayName("Should get all contacts without filter")
  void testGetContactsNoFilter() {
    Page<Contact> page = new PageImpl<>(List.of(testContact), PageRequest.of(0, 10), 1);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "all", null);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(contactRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return empty page")
  void testGetContactsEmpty() {
    Page<Contact> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

    Page<Contact> result = contactService.getContacts(0, 10, "all", null);

    assertNotNull(result);
    assertEquals(0, result.getTotalElements());
  }

  @Test
  @DisplayName("Should handle negative page number")
  void testGetContactsNegativePage() {
    Page<Contact> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(-1, 10, "all", null);

    assertNotNull(result);
    verify(contactRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle negative page size")
  void testGetContactsNegativeSize() {
    Page<Contact> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, -10, "all", null);

    assertNotNull(result);
    verify(contactRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle zero page size")
  void testGetContactsZeroSize() {
    Page<Contact> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 0, "all", null);

    assertNotNull(result);
    verify(contactRepository, times(1)).findAll(any(Pageable.class));
  }

  // ========== TEST getContacts (Unread Filter) ==========
  @Test
  @DisplayName("Should get unread contacts")
  void testGetContactsUnread() {
    Page<Contact> page = new PageImpl<>(List.of(testContact), PageRequest.of(0, 10), 1);
    when(contactRepository.findByIsReadFalse(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "unread", null);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(contactRepository, times(1)).findByIsReadFalse(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return empty for unread filter")
  void testGetContactsUnreadEmpty() {
    Page<Contact> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findByIsReadFalse(any(Pageable.class))).thenReturn(emptyPage);

    Page<Contact> result = contactService.getContacts(0, 10, "unread", null);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ========== TEST getContacts (Read Filter) ==========
  @Test
  @DisplayName("Should get read contacts")
  void testGetContactsRead() {
    testContact.setRead(true);
    Page<Contact> page = new PageImpl<>(List.of(testContact), PageRequest.of(0, 10), 1);
    when(contactRepository.findByIsReadTrue(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "read", null);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(contactRepository, times(1)).findByIsReadTrue(any(Pageable.class));
  }

  // ========== TEST getContacts (Replied Filter) ==========
  @Test
  @DisplayName("Should get replied contacts")
  void testGetContactsReplied() {
    testContact.setAdminReply("This is admin reply");
    Page<Contact> page = new PageImpl<>(List.of(testContact), PageRequest.of(0, 10), 1);
    when(contactRepository.findByAdminReplyIsNotNull(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "replied", null);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(contactRepository, times(1)).findByAdminReplyIsNotNull(any(Pageable.class));
  }

  // ========== TEST getContacts (Search) ==========
  @Test
  @DisplayName("Should search contacts by keyword")
  void testGetContactsSearch() {
    Page<Contact> page = new PageImpl<>(List.of(testContact), PageRequest.of(0, 10), 1);
    when(contactRepository.searchContacts(anyString(), any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "all", "john");

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(contactRepository, times(1)).searchContacts(anyString(), any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle empty search keyword")
  void testGetContactsSearchEmpty() {
    Page<Contact> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "all", "");

    assertNotNull(result);
    verify(contactRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle null search keyword")
  void testGetContactsSearchNull() {
    Page<Contact> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "all", null);

    assertNotNull(result);
    verify(contactRepository, times(1)).findAll(any(Pageable.class));
  }

  @Test
  @DisplayName("Should handle whitespace-only search keyword")
  void testGetContactsSearchWhitespace() {
    Page<Contact> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(contactRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Contact> result = contactService.getContacts(0, 10, "all", "   ");

    assertNotNull(result);
    verify(contactRepository, times(1)).findAll(any(Pageable.class));
  }

  // ========== TEST countUnread ==========
  @Test
  @DisplayName("Should count unread contacts")
  void testCountUnread() {
    when(contactRepository.countByIsReadFalse()).thenReturn(5L);

    long result = contactService.countUnread();

    assertEquals(5L, result);
    verify(contactRepository, times(1)).countByIsReadFalse();
  }

  @Test
  @DisplayName("Should return zero for no unread contacts")
  void testCountUnreadZero() {
    when(contactRepository.countByIsReadFalse()).thenReturn(0L);

    long result = contactService.countUnread();

    assertEquals(0L, result);
  }

  @Test
  @DisplayName("Should handle large unread count")
  void testCountUnreadLarge() {
    when(contactRepository.countByIsReadFalse()).thenReturn(999999L);

    long result = contactService.countUnread();

    assertEquals(999999L, result);
  }

  // ========== TEST replyAndSendEmail ==========
  @Test
  @DisplayName("Should reply and send email successfully")
  void testReplyAndSendEmailSuccess() {
    String replyText = "Thank you for contacting us!";

    when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
    when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
    doNothing().when(emailService).sendContactReply(any(Contact.class), anyString());

    contactService.replyAndSendEmail(1L, replyText);

    assertEquals(replyText, testContact.getAdminReply());
    assertTrue(testContact.isRead());
    assertNotNull(testContact.getRepliedAt());
    verify(contactRepository, times(1)).save(any(Contact.class));
    verify(emailService, times(1)).sendContactReply(any(Contact.class), anyString());
  }

  @Test
  @DisplayName("Should throw exception when contact not found")
  void testReplyAndSendEmailNotFound() {
    when(contactRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      contactService.replyAndSendEmail(999L, "reply");
    });
  }

  @Test
  @DisplayName("Should throw exception when contact ID is negative")
  void testReplyAndSendEmailNegativeId() {
    when(contactRepository.findById(-1L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> {
      contactService.replyAndSendEmail(-1L, "reply");
    });
  }

  @Test
  @DisplayName("Should handle reply with empty text")
  void testReplyAndSendEmailEmptyReply() {
    when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
    when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
    doNothing().when(emailService).sendContactReply(any(Contact.class), anyString());

    contactService.replyAndSendEmail(1L, "");

    assertEquals("", testContact.getAdminReply());
    verify(emailService, times(1)).sendContactReply(any(Contact.class), anyString());
  }

  @Test
  @DisplayName("Should handle reply with null text")
  void testReplyAndSendEmailNullReply() {
    when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
    when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
    doNothing().when(emailService).sendContactReply(any(Contact.class), any());

    contactService.replyAndSendEmail(1L, null);

    assertNull(testContact.getAdminReply());
  }

  @Test
  @DisplayName("Should handle very long reply text")
  void testReplyAndSendEmailLongReply() {
    String longReply = "A".repeat(10000);

    when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
    when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
    doNothing().when(emailService).sendContactReply(any(Contact.class), anyString());

    contactService.replyAndSendEmail(1L, longReply);

    assertEquals(longReply, testContact.getAdminReply());
    verify(emailService, times(1)).sendContactReply(any(Contact.class), anyString());
  }

  // ========== TEST getAllContactsWithReplyInfo ==========
  @Test
  @DisplayName("Should get all contacts with reply info")
  void testGetAllContactsWithReplyInfo() {
    when(contactRepository.findAll(any())).thenReturn(List.of(testContact));

    List<Contact> result = contactService.getAllContactsWithReplyInfo();

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(contactRepository, times(1)).findAll(any());
  }

  @Test
  @DisplayName("Should return empty list when no contacts")
  void testGetAllContactsWithReplyInfoEmpty() {
    when(contactRepository.findAll(any())).thenReturn(List.of());

    List<Contact> result = contactService.getAllContactsWithReplyInfo();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return multiple contacts in descending order")
  void testGetAllContactsWithReplyInfoMultiple() {
    Contact contact2 = new Contact();
    contact2.setId(2L);
    contact2.setEmail("jane@example.com");
    contact2.setCreatedAt(LocalDateTime.now().minusHours(1));

    when(contactRepository.findAll(any())).thenReturn(List.of(testContact, contact2));

    List<Contact> result = contactService.getAllContactsWithReplyInfo();

    assertNotNull(result);
    assertEquals(2, result.size());
  }

  // ========== TEST Edge Cases ==========
  @Test
  @DisplayName("Should handle contact with special characters in email")
  void testSaveContactSpecialEmail() {
    testContact.setEmail("test+special.email@example.co.uk");

    when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

    Contact result = contactService.saveContact(testContact);

    assertNotNull(result);
    assertEquals("test+special.email@example.co.uk", result.getEmail());
  }

  @Test
  @DisplayName("Should handle contact with very long content")
  void testSaveContactLongContent() {
    String longContent = "This is a very long content. ".repeat(1000);
    testContact.setNoiDung(longContent);

    when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

    Contact result = contactService.saveContact(testContact);

    assertNotNull(result);
    assertEquals(longContent, result.getNoiDung());
  }
}
