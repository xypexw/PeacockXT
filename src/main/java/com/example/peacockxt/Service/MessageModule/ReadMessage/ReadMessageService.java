package com.example.peacockxt.Service.MessageModule.ReadMessage;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
public class ReadMessageService {

    private final ReadMessageFromCache readMessageFromCache;
    private final ReadMessageFromDatabase readMessageFromDatabase;

    public ReadMessageService(ReadMessageFromCache readMessageFromCache, ReadMessageFromDatabase readMessageFromDatabase) {
        this.readMessageFromCache = readMessageFromCache;
        this.readMessageFromDatabase = readMessageFromDatabase;
    }

    public List<MessageResponse> loadPastMessage(Long pivotId,String channelId){
        try{
            return readMessageFromDatabase.readDbMessages(channelId,pivotId);
        }
        catch(DatabaseException e){
            throw new BusinessLogicException("Database error",e);
        }
        catch (Exception e){
            throw new BusinessLogicException("Cannot read messages from DB",e);
        }
    }

    public List<MessageResponse> readNormalMessage(long pivotId,String channelId,int limit){
        try{
            List<MessageResponse> result = new ArrayList<>();
            int maxCapacityCache = 120;
            if(limit < maxCapacityCache) {
                List<Long> indexes = readMessageFromDatabase.readIndexFormDatabase(pivotId,channelId);

            }
            else{
                result = readMessageFromDatabase.readDbMessages(channelId,pivotId);
            }
            return result;
        }
        catch(CacheAccessException e){
            throw new BusinessLogicException("Cache error",e);
        }
        catch(Exception e){
            throw new BusinessLogicException("Cannot read messages from DB",e);
        }
    }

}
