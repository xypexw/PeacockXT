package com.example.peacockxt.Service.PrivateSession;


import com.example.peacockxt.DTO.Respone.PrivateSessionResponse;
import com.example.peacockxt.Infrastructure.Cache.PrivateSessionRedisDao;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.PrivateSession;
import com.example.peacockxt.Repository.Imlpementation.PrivateSessionRepository;
import com.example.peacockxt.Service.Provider.UserProvider;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Service
@AllArgsConstructor
public class ReadPrivateSession {
    private final PrivateSessionRedisDao privateSessionRedisDao;
    private final PrivateSessionRepository privateSessionRepository;
    private final UserProvider userProvider;

    public PrivateSessionResponse getSinglePrivateSessionByUser(Long userId , Long targetId ) {

        if(!userProvider.hasUser(userId))
            throw new BusinessLogicException("User does not exist");

        if(!userProvider.hasUser(targetId))
            throw new BusinessLogicException("Target User does not exist");

        List<Long> indexes = getPrivateSessionIds(userId);
        List<PrivateSession> privateSessions = getPrivateSessionByIds(indexes);
        PrivateSession foundSession = getPrivateSession(userId, targetId, privateSessions);
        PrivateSessionResponse privateSessionResponse = new PrivateSessionResponse();
        privateSessionResponse.setPartnerId(targetId);
        privateSessionResponse.setPrivateSessionId(foundSession.getId());
        return privateSessionResponse;

    }

    @NotNull
    private static PrivateSession getPrivateSession(Long userId, Long targetId, List<PrivateSession> privateSessions) {
        Long smallerId = min(userId, targetId);
        Long largerId = max(userId, targetId);
        PrivateSession foundSession = null;
        for(PrivateSession privateSession : privateSessions) {
            if( privateSession.getConnectorId().equals(smallerId) &&  privateSession.getReceiverId().equals(largerId) ) {
                foundSession = privateSession;
                break;
            }
        }
        if(foundSession == null){
            throw new  BusinessLogicException("Private session not found");
        }
        return foundSession;
    }

    public List<PrivateSessionResponse> getPrivateSessionByUser(Long userId) {

        if(!userProvider.hasUser(userId)) throw new BusinessLogicException("User does not exist");

        List<Long> indexes = getPrivateSessionIds(userId);
        List<PrivateSession> privateSessions = getPrivateSessionByIds(indexes);
        return privateSessionResponseBuilder(privateSessions,userId);
    }

    private List<Long> getPrivateSessionIds(Long userId) {
        Set<Long> ids = privateSessionRedisDao.getAllSessionInbox(userId);
        if( ids!=null && !ids.isEmpty() ){
            return new ArrayList<>(ids);
        }
        List<Long> result = privateSessionRepository.getPrivateSessionIdsByUserId(userId);
        if(result!=null && !result.isEmpty()){
            privateSessionRedisDao.saveAllSessionIdsToInbox(userId,result);
            return result;
        }
        return Collections.emptyList();
    }

    private List<PrivateSession> getPrivateSessionByIds(List<Long> ids) {
        List<PrivateSession> privateSessionsInCache = privateSessionRedisDao.getSessionBatch(ids);
        List<Long> missingIds = new ArrayList<>();

        for(int i = 0 ; i < privateSessionsInCache.size() ; i++){
            if(privateSessionsInCache.get(i)==null){
                missingIds.add(ids.get(i));
            }
        }
        List<PrivateSession> privateSessionsInDatabase = privateSessionRepository.getPrivateSessionListByIds(missingIds);
        Map<Long,PrivateSession> map = new HashMap<>();
        for(PrivateSession privateSession : privateSessionsInDatabase){
            map.put(privateSession.getId(),privateSession);
        }
        privateSessionRedisDao.saveSessionInfoBatch(privateSessionsInDatabase);
        List<PrivateSession> result = new ArrayList<>();
        for(int i = 0 ; i < ids.size() ; i++){
            PrivateSession privateSession = privateSessionsInCache.get(i);
            if(privateSession==null && map.containsKey(ids.get(i))){
                privateSession =  map.get(ids.get(i));
            }
            if(privateSession!=null){
                result.add(privateSession);
            }
        }
        return result;
    }

    List<PrivateSessionResponse> privateSessionResponseBuilder(List<PrivateSession> privateSessions , Long userId ){
        List<PrivateSessionResponse> result = new ArrayList<>();
        for(PrivateSession privateSession : privateSessions){
            if(privateSession==null) continue;
            PrivateSessionResponse privateSessionResponse = new PrivateSessionResponse();
            privateSessionResponse.setPrivateSessionId(privateSession.getId());
            if(privateSession.getConnectorId().equals(userId)){
                privateSessionResponse.setPartnerId(privateSession.getReceiverId());
            }
            else privateSessionResponse.setPartnerId(privateSession.getConnectorId());
            result.add(privateSessionResponse);
        }
        return result;
    }

}
