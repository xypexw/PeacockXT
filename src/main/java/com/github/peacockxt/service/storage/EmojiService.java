package com.github.peacockxt.service.storage;

import com.github.peacockxt.dto.event.EmojiPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.dto.response.EmojiResponse;
import com.github.peacockxt.infrastructure.cache.EmojiRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheCustomEmoji;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.CustomEmoji;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.CustomEmojiRepository;
import com.github.peacockxt.repository.implementation.TeamRepository;
import com.github.peacockxt.service.RedisRetryManager;
import com.github.peacockxt.service.SnowflakeIdGenerator;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.TeamProvider;
import com.github.peacockxt.service.provider.UserProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class EmojiService {

    private final EmojiRedisDao emojiRedisDao;
    private final CustomEmojiRepository customEmojiRepository;
    private final TeamRepository teamRepository;
    private final LocalEventDispatcher localEventDispatcher;
    private final TeamProvider teamProvider;
    private final UserProvider userProvider;
    private final MembershipProvider membershipProvider;
    private final RedisRetryManager redisRetryManager;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final String DEFAULT_STATUS = "DEFAULT";
    private final String DELETED_STATUS = "DELETED";
    private final String CREATE_EVENT = "CREATE_EMOJI";
    private final String DELETE_EVENT = "DELETE_EMOJI";

    @Transactional
    public void createEmoji(Long teamId, Long userId, String name, String objectKey) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("team does not exist");
        }
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("user does not exist");
        }

        CustomEmoji customEmoji = new CustomEmoji();
        customEmoji.setEmojiId(snowflakeIdGenerator.nextId());
        customEmoji.setCreateBy(userId);
        customEmoji.setTeam(teamRepository.getReferenceById(teamId));
        customEmoji.setTeamId(teamId);
        customEmoji.setImageUrl(objectKey);
        customEmoji.setEmojiName(name);
        customEmoji.setStatus(DEFAULT_STATUS);
        customEmojiRepository.save(customEmoji);

        EmojiPayload payload = EmojiPayload.builder().emojiId(customEmoji.getEmojiId())
                .imageUrl(customEmoji.getImageUrl())
                .status(DEFAULT_STATUS).build();

        localEventDispatcher.dispatchToRabbitMQ(InternalEvent.<EmojiPayload>builder().payload(payload)
                .eventType(CREATE_EVENT).destination(WebsocketDestination.TEAM.getDestination(teamId)).build());

        CacheCustomEmoji cacheEmoji = toCacheEmoji(customEmoji);
        executeAfterCommit(() -> emojiRedisDao.saveEmoji(teamId, cacheEmoji));
    }

    @Transactional
    public void deleteEmoji(Long teamId, Long userId, String emojiName) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("team does not exist");
        }
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("user does not exist");
        }
        CustomEmoji customEmoji = customEmojiRepository.getCustomEmojiByTeamIdAndEmojiName(teamId, emojiName);
        if (customEmoji == null) {
            throw new BusinessLogicException("emoji does not exist");
        }
        customEmoji.setStatus(DELETED_STATUS);
        customEmojiRepository.save(customEmoji);

        EmojiPayload payload = EmojiPayload.builder().emojiId(customEmoji.getEmojiId()).status(DELETED_STATUS).build();

        localEventDispatcher.dispatchToRabbitMQ(InternalEvent.<EmojiPayload>builder().payload(payload)
                .eventType(DELETE_EVENT).destination(WebsocketDestination.TEAM.getDestination(teamId)).build());

        executeAfterCommit(() -> emojiRedisDao.deleteEmoji(teamId, emojiName));
    }

    public List<EmojiResponse> getAllCustomEmojisOfTeam(Long teamId, Long userId) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("team does not exist");
        }
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("user does not exist");
        }
        if (membershipProvider.getMembershipByUserAndTeam(userId, teamId) == null) {
            throw new BusinessLogicException("User does not belong to this team");
        }

        Map<String, CacheCustomEmoji> map = emojiRedisDao.getAllEmojis(teamId);
        if (map != null && !map.isEmpty()) {
            List<CacheCustomEmoji> emojis = new ArrayList<>(map.values());
            return emojiResponsesBuilder(emojis);
        }

        List<CustomEmoji> customEmojiList = customEmojiRepository.getCustomEmojisByTeamId(teamId, DEFAULT_STATUS);
        List<CacheCustomEmoji> cacheEmojis = new ArrayList<>(customEmojiList.size());
        for (CustomEmoji entity : customEmojiList) {
            cacheEmojis.add(toCacheEmoji(entity));
        }
        if (!cacheEmojis.isEmpty()) {
            emojiRedisDao.setAllEmoji(teamId, cacheEmojis);
        }
        return emojiResponsesBuilder(cacheEmojis);
    }

    private void executeAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        task.run();
                    } catch (Exception e) {
                        log.error("Failed to execute task after transaction commit: {}", e.getMessage(), e);
                        redisRetryManager.execute(task);
                    }
                }
            });
        } else {
            redisRetryManager.execute(task);
        }
    }

    List<EmojiResponse> emojiResponsesBuilder(List<CacheCustomEmoji> customEmojiList) {
        List<EmojiResponse> emojiResponses = new ArrayList<>();
        for (CacheCustomEmoji customEmoji : customEmojiList) {
            if (customEmoji == null || DELETED_STATUS.equals(customEmoji.getStatus())) {
                continue;
            }
            EmojiResponse emojiResponse = new EmojiResponse();
            emojiResponse.setEmojiName(customEmoji.getEmojiName());
            emojiResponse.setObjectName(customEmoji.getImageUrl());
            emojiResponses.add(emojiResponse);
        }
        return emojiResponses;
    }

    private CacheCustomEmoji toCacheEmoji(CustomEmoji entity) {
        return CacheCustomEmoji.builder()
                .emojiId(entity.getEmojiId())
                .emojiName(entity.getEmojiName())
                .imageUrl(entity.getImageUrl())
                .createBy(entity.getCreateBy())
                .teamId(entity.getTeamId())
                .status(entity.getStatus())
                .build();
    }
}
