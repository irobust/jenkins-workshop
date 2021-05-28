# Connect to Git repository
We'll need to get a personal API key from our git repo
1. create a fork of this repository:

https://github.com/irobust/jenkins-git.git

2. creating
https://github.com/{your_github_user_name}/jenkins-git.git
in github, get a personal access token:

https://github.com/settings/tokens

3. Go to Developer Settings -> Personal access token
4. Click Generate new token
5. when creating the token, add:
```
        admin:repo_hook
        repo:*
        notifications:*
```

6. Go to Github Server section (in system configuration page) and add new credentials

7. Create multi-branch project
8. Add github credential 

9. after configuring jenkins, verify that the webhook was created
https://github.com/{your_github_user_name}/jenkins-git/settings/hooks


