package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.exception.UpgradResponseStatusException;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.TestStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
class ConsultationControllerTest {


    @Autowired
    ConsultationController consultationController;


    @Autowired
    TestRequestQueryService testRequestQueryService;


  @Test
  @WithUserDetails(value = "doctor")
  public void  calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status() {
    TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);
    TestRequest resultTestRequest = consultationController.assignForConsultation(testRequest.requestId);
    assertNotNull(resultTestRequest);
    assertThat(testRequest.requestId, equalTo(resultTestRequest.getRequestId()));
    assertThat(resultTestRequest.getStatus(), equalTo(RequestStatus.DIAGNOSIS_IN_PROCESS));
  }

  public TestRequest getTestRequestByStatus(RequestStatus status) {
      return testRequestQueryService.findBy(status).stream().findFirst().get();
  }

  @Test
  @WithUserDetails(value = "doctor")
  public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception() {
    Long InvalidRequestId = -34L;
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              consultationController.assignForConsultation(InvalidRequestId);
            });
    assertThat(exception.getMessage(), containsString("Invalid ID"));
  }

  @Test
  @WithUserDetails(value = "doctor")
  public void  calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details() {

    TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
    CreateConsultationRequest consultationRequest = getCreateConsultationRequest(testRequest);
    TestRequest resultTestRequest = consultationController.updateConsultation(testRequest.requestId, consultationRequest);

    assertThat(testRequest.requestId, equalTo(resultTestRequest.requestId));
    assertThat(resultTestRequest.getStatus(), equalTo(RequestStatus.COMPLETED));
    assertThat(testRequest.getConsultation().getSuggestion(),equalTo(resultTestRequest.getConsultation().getSuggestion()));
  }

  @Test
  @WithUserDetails(value = "doctor")
  public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception() {
    TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
    CreateConsultationRequest consultationRequest = getCreateConsultationRequest(testRequest);
    ResponseStatusException exception =
            assertThrows(
                    ResponseStatusException.class,
                    () -> {
                      consultationController.updateConsultation(-34L, consultationRequest);
                    });
    assertThat(exception.getMessage(), containsString("Invalid ID"));

  }

  @Test
  @WithUserDetails(value = "doctor")
  public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception() {

    TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
    CreateConsultationRequest consultationRequest = getCreateConsultationRequest(testRequest);
    consultationRequest.setSuggestion(null);
    UpgradResponseStatusException exception =
            assertThrows(
                    UpgradResponseStatusException.class,
                    () -> {
                      consultationController.updateConsultation(testRequest.getRequestId(), consultationRequest);
                    });
    assertNotNull(exception);
    assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
  }

  public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
    CreateConsultationRequest createConsultationRequest = new CreateConsultationRequest();

    if (testRequest.getLabResult().getResult() == TestStatus.POSITIVE) {
      createConsultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
      createConsultationRequest.setComments("Needs home quarantine");
    } else {
      createConsultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
      createConsultationRequest.setComments("OK");
    }

    return createConsultationRequest;
  }
}