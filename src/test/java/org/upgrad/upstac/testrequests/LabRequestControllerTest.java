package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.exception.UpgradResponseStatusException;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.TestStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
class LabRequestControllerTest {

    @Autowired
    LabRequestController labRequestController;

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.INITIATED);
        TestRequest resultTestRequest = labRequestController.assignForLabTest(testRequest.getRequestId());
        assertNotNull(resultTestRequest);
        assertThat(testRequest.getRequestId(), equalTo(resultTestRequest.getRequestId()));
        assertThat(resultTestRequest.getStatus(), equalTo(RequestStatus.LAB_TEST_IN_PROGRESS));
        assertNotNull(resultTestRequest.getLabResult());
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){

        Long InvalidRequestId= -34L;
        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> {
                            labRequestController.assignForLabTest(InvalidRequestId);
                        });
        assertThat(exception.getMessage(), containsString("Invalid ID"));

    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        TestRequest resultTestResult = labRequestController.updateLabTest(testRequest.requestId, createLabResult);
        assertThat(testRequest.requestId, equalTo(resultTestResult.requestId));
        assertThat(resultTestResult.getStatus(), equalTo(RequestStatus.LAB_TEST_COMPLETED));
        assertThat(createLabResult.getResult(), equalTo(resultTestResult.getLabResult().getResult()));

    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        Long InvalidRequestId= -34L;

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> {
                            labRequestController.updateLabTest(InvalidRequestId, createLabResult);
                        });
        assertThat(exception.getMessage(), containsString("Invalid ID"));

    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        createLabResult.setResult(null);
        UpgradResponseStatusException exception =
                assertThrows(
                        UpgradResponseStatusException.class,
                        () -> {
                            labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);
                        });
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString("ConstraintViolationException"));
    }

    public CreateLabResult getCreateLabResult(TestRequest testRequest) {

        CreateLabResult createLabResult = new CreateLabResult();
        createLabResult.setResult(TestStatus.NEGATIVE);
        createLabResult.setComments("blood test not done");
        createLabResult.setBloodPressure("120");
        createLabResult.setHeartBeat("100");
        createLabResult.setTemperature("99");
        createLabResult.setOxygenLevel("98");
        return createLabResult;
    }

}