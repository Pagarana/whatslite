# Firebase RTDB: Missing `/users` Node — Root Cause & Fix

## Summary
If `/users` is missing or malformed, the online user list and room discovery will not work.  
This guide shows **exact JSON** and **safe import paths**.

---

## 1) Required Database Rules (publish in *Rules* tab)
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "users": { ".indexOn": ["nickname", "isOnline"] },
    "rooms": {
      "$roomId": {
        "messages": { ".indexOn": ["timestamp"] }
      }
    }
  }
}
