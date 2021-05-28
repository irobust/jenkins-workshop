# Step by Step
1. docker-compose up -d
2. Open Mailhog - go to http://localhost:8025
3. Open Jenkins - go to http://localhost:9080
4. docker-compose logs jenkins (Get intitialAdminPassword)
5. Set Mail Server
   Extend Email Notification (Set following options)
```
    SMTP Server = mails
    SMTP Port   = 1025
```