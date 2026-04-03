# Backend Structure

This project now uses a flat package layout under `com.example`.

## Root package
`com.example`

## Files directly in the package
- `UserController.java`
- `UserService.java`
- `UserRepository.java`
- `PostController.java`
- `PostService.java`
- `PostRepository.java`
- `CommentController.java`
- `CommentService.java`
- `CommentRepository.java`
- `VoteController.java`
- `VoteService.java`
- `VoteRepository.java`
- `AdminController.java`
- `AdminService.java`
- `AdminRepository.java`

## Notes
- No `features/` folder
- No subfolders like `user/controller` or `post/service`
- All route/domain files are directly accessible in one package

## Recommended implementation order
1. user (register/login/me)
2. post (create/list/detail)
3. comment (list/create/delete)
4. vote (up/down with unique vote per user+post)
5. admin (moderation/pin/role operations)

