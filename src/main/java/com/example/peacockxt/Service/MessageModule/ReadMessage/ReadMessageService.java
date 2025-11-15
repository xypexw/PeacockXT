package com.example.peacockxt.Service.MessageModule.ReadMessage;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import com.example.peacockxt.Service.MessageModule.CacheModule.ReadMessageFromCache;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import org.slf4j.Logger;


@Service

public class ReadMessageService {
    private static final Logger log = LoggerFactory.getLogger(ReadMessageService.class);
    private final ReadMessageFromCache readMessageFromCache;
    private final ReadMessageFromDatabase readMessageFromDatabase;

    public ReadMessageService(ReadMessageFromCache readMessageFromCache,
                              ReadMessageFromDatabase readMessageFromDatabase) {
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

    public Message getMessage(Long messageId){
        try{
            return readMessageFromDatabase.getMessage(messageId);
        }
        catch(DatabaseException e){
            throw new BusinessLogicException("Database error in read message ",e);
        }
    }

    public List<MessageResponse> readNormalMessage(long pivotId, String channelId) {
        try {
            // 1. Đọc danh sách index từ DB (critical)
            List<Long> indexes = readMessageFromDatabase.readIndexFormDatabase(pivotId, channelId);
            // 3. Cố read cache
            List<MessageResponse> cached = null;
            try {
                cached = readMessageFromCache.readCacheMessage(indexes, channelId);
                // 4. Nếu cache có dữ liệu → trả về luôn
                if (cached != null) {
                    log.info("Return Cache for channelId={} pivotId={} size={}", channelId, pivotId, cached.size());
                    return cached; // trả về cả trường hợp rỗng
                }
            } catch (CacheAccessException e) {
                log.warn("Cache access failed for channelId={} pivotId={}. Reason={}",
                        channelId, pivotId, e.getMessage());
            }
            // 5. Fallback DB
            log.debug("Failback DB for channelId={} pivotId={}. Falling back to DB...", channelId, pivotId);
            return readMessageFromDatabase.readDbMessages(channelId, pivotId);
        }
        catch (DatabaseException e) {
            // Tất cả lỗi DB đều được bọc thành BusinessLogicException
            log.warn(" Database crash channelId={} pivotId={}. Reason={}",
                    channelId, pivotId, e.getMessage());
            throw new BusinessLogicException("Database error during readNormalMessage()", e);
        }
        catch (Exception e) {
            // Các lỗi bất ngờ khác
            throw new BusinessLogicException("Unexpected error while reading messages", e);
        }
    }
}
