package gov.ca.emsa.pulse.broker.manager.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.ca.emsa.pulse.broker.dao.QueryDAO;
import gov.ca.emsa.pulse.broker.dto.QueryDTO;
import gov.ca.emsa.pulse.broker.dto.QueryStatus;
import gov.ca.emsa.pulse.broker.manager.QueryManager;

@Service
public class QueryManagerImpl implements QueryManager{
	@Autowired QueryDAO queryDao;
	
	@Override
	public List<QueryDTO> getAllQueriesForUser(String userKey) {
		return queryDao.findAllForUser(userKey);
	}

	@Override
	public List<QueryDTO> getActiveQueriesForUser(String userKey) {
		return queryDao.findAllForUserWithStatus(userKey, QueryStatus.ACTIVE.name());
	}

	@Override
	public QueryDTO getQueryStatusDetails(Long queryId) {
		return queryDao.getById(queryId);
	}
	
	@Override
	public String getQueryStatus(Long queryId) {
		QueryDTO query = queryDao.getById(queryId);
		return query.getStatus();
	}
	
}
