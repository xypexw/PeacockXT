package com.github.peacockxt.repository.implementation;

import com.github.peacockxt.models.CustomEmoji;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CustomEmojiRepository extends JpaRepository<CustomEmoji,Long> {

    @Query( "Select e from CustomEmoji e where e.team.teamId = :teamId and e.emojiName = :EmojiName ")
    public CustomEmoji getCustomEmojiByTeamIdAndEmojiName(@Param("teamId") Long teamId,@Param("EmojiName") String emojiName);

    @Query( "Select e from CustomEmoji e where e.team.teamId = :teamId and e.status = :status ")
    public List<CustomEmoji> getCustomEmojisByTeamId(@Param("teamId") Long teamId,@Param("status")  String status);

}
