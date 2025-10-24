{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",

    "users": {
      ".indexOn": ["nickname", "isOnline"]
    },

    "rooms": {
      "$roomId": {
        "messages": {
          ".indexOn": ["timestamp"]
        }
      }
    }
  }
}
