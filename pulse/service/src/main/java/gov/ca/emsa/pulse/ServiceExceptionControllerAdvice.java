package gov.ca.emsa.pulse;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import gov.ca.emsa.pulse.auth.user.UserRetrievalException;
import gov.ca.emsa.pulse.common.domain.ErrorJSONObject;
import gov.ca.emsa.pulse.service.BrokerError;


@ControllerAdvice
public class ServiceExceptionControllerAdvice {
	private static final Logger logger = LogManager.getLogger(ServiceExceptionControllerAdvice.class);

	@ExceptionHandler(HttpServerErrorException.class)
	public ResponseEntity<ErrorJSONObject> exception(HttpServerErrorException e) {
		String responseBody = e.getResponseBodyAsString();
		String errorMessage = responseBody;
    	ObjectReader reader = new ObjectMapper().reader().forType(BrokerError.class);
    	try {
    		BrokerError brokerError = reader.readValue(responseBody);
    		errorMessage = brokerError.getMessage();
    	} catch(IOException ex) {
    		logger.warn("Could not turn " + responseBody + " into BrokerError object", ex);
    	}
		logger.error(errorMessage);
		
		return new ResponseEntity<ErrorJSONObject>(new ErrorJSONObject(errorMessage), e.getStatusCode());
	}
	
	@ExceptionHandler(HttpClientErrorException.class)
	public ResponseEntity<ErrorJSONObject> exception(HttpClientErrorException e) {
		String responseBody = e.getResponseBodyAsString();
		String errorMessage = responseBody;
    	ObjectReader reader = new ObjectMapper().reader().forType(BrokerError.class);
    	try {
    		BrokerError brokerError = reader.readValue(responseBody);
    		errorMessage = brokerError.getError();
    	} catch(IOException ex) {
    		logger.warn("Could not turn " + responseBody + " into BrokerError object", ex);
    	}
		logger.error(errorMessage);
		
		return new ResponseEntity<ErrorJSONObject>(new ErrorJSONObject(errorMessage), e.getStatusCode());
	}
	
	@ExceptionHandler(UserRetrievalException.class)
	public ResponseEntity<ErrorJSONObject> exception(UserRetrievalException e) {
		logger.error(e.getMessage(), e);
		return new ResponseEntity<ErrorJSONObject>(new ErrorJSONObject(e.getMessage()), HttpStatus.UNAUTHORIZED);
	}
}
