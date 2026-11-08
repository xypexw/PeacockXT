package com.example.peacockxt.Policy;
import com.example.peacockxt.Models.Comment;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.Membership;
import com.example.peacockxt.Models.Post;
import org.springframework.stereotype.Component;

@Component
public class CommentPolicy {

    private static final String DELETED_STATUS = "DELETED";
    private static final String READER_ROLE = "READER";

    public void validateCreateComment(Long userId, Membership membership, Post post) {

        // 1. Kiểm tra Membership có tồn tại hay không
        if (membership == null) {
            throw new BusinessLogicException("Membership is not found");
        }

        // 2. Kiểm tra Post có tồn tại hay không
        if (post == null) {
            throw new BusinessLogicException("Post is not found");
        }

        // 3. Không cho phép tạo comment trên bài viết đã bị xóa
        if (DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        // 4. Đảm bảo membership thực sự thuộc về user
        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID does not match membership");
        }

        // 5. Đảm bảo membership và post thuộc cùng một team
        if (!membership.getTeamId().equals(post.getTeam().getTeamId())) {
            throw new BusinessLogicException("Team ID does not match post's team");
        }

        // 6. Kiểm tra quyền comment chỉ Role READER chỉ có quyền đọc, không được tạo comment
        if (READER_ROLE.equals(membership.getRole())) {
            throw new BusinessLogicException("User does not have permission to comment");
        }
    }

    public void validateUpdateComment(Long userId, Membership membership, Post post, Comment comment) {
        // 1. Kiểm tra Comment có tồn tại hoặc bị xóa chưa
        if (comment == null || DELETED_STATUS.equals(comment.getStatus())) {
            throw new BusinessLogicException("Comment not found or deleted");
        }

        // 2. Kiểm tra Post có tồn tại hoặc bị xóa chưa
        if (post == null || DELETED_STATUS.equals(post.getStatus())) {
            throw new BusinessLogicException("Post has been deleted");
        }

        // 3. Kiểm tra Membership có hợp lệ không
        if (membership == null) {
            throw new BusinessLogicException("Membership is not found");
        }
        if (!membership.getUserId().equals(userId)) {
            throw new BusinessLogicException("User ID does not match membership");
        }

        // 4. Kiểm tra Team ID có đồng nhất giữa Membership và Post không
        if (!membership.getTeam().getTeamId().equals(post.getTeam().getTeamId())) {
            throw new BusinessLogicException("Team ID does not match post's team");
        }

        // 5. Kiểm tra quyền của Member trong Team , nếu Reader thì không có quyền comment
        if (READER_ROLE.equals(membership.getRole())) {
            throw new BusinessLogicException("User does not have permission to update comment");
        }

        // 6. [QUAN TRỌNG NHẤT]: Chỉ chính chủ (người tạo comment) mới được phép sửa
        if (!comment.getCreateBy().equals(userId)) {
            throw new BusinessLogicException("Only the creator can edit this comment");
        }
    }

    public void validateReadComment( Long userId , Long teamId , Membership membership, Post post) {
        if(post==null || membership==null){
            throw new BusinessLogicException("Post or Membership not found");
        }
        if(!membership.getTeamId().equals(teamId)){
            throw new BusinessLogicException("Team Id mismatch");
        }
        if(!membership.getUserId().equals(userId)){
            throw new BusinessLogicException("User Id mismatch");
        }

        if(!post.getTeam().getTeamId().equals(teamId)){
            throw new BusinessLogicException("Team Id of Post mismatch ");
        }
    }


}