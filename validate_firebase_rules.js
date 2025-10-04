// Firebase Rules Validation ve Test Kodu
// Bu kodu Firebase Console > Database > Rules sekmesindeki Simulator ile test edebilirsiniz

const testRules = {
  "rules": {
    ".read": true,
    ".write": true,
    "users": {
      ".indexOn": ["nickname", "isOnline"]
    },
    "messages": {
      ".indexOn": ["to", "from", "chatRoomId", "timestamp"]
    },
    "chatRooms": {
      ".indexOn": ["participants", "lastMessage"]  
    }
  }
};

// Test Scenarios:
const testScenarios = [
  {
    name: "User Write Test",
    path: "/users/test1",
    method: "set",
    data: {
      "nickname": "test1",
      "language": "tr",
      "isOnline": true,
      "lastSeen": 1699999999999
    }
  },
  {
    name: "Message Write Test", 
    path: "/messages/msg123",
    method: "set",
    data: {
      "from": "test1",
      "to": "test2",
      "message": "Hello",
      "chatRoomId": "room123",
      "timestamp": 1699999999999,
      "senderLanguage": "tr"
    }
  },
  {
    name: "Messages Query Test",
    path: "/messages",
    method: "get",
    query: {
      "orderBy": "to",
      "equalTo": "test2"
    }
  }
];

console.log("Firebase Rules JSON:");
console.log(JSON.stringify(testRules, null, 2));

console.log("\n=== Manuel Steps ===");
console.log("1. Firebase Console'a git: https://console.firebase.google.com/project/whatslite-4377b");
console.log("2. Realtime Database sekmesini aç");
console.log("3. Rules sekmesine git");
console.log("4. Yukarıdaki rules JSON'ı yapıştır");
console.log("5. Publish butonuna bas");
console.log("6. Rules Simulator ile test et");

console.log("\n=== Index Warning Fix ===");
console.log("Eğer index warning görürsen:");
console.log("- Firebase Console > Database > Usage sekmesinde");
console.log("- Index recommendation'ları göreceksin");
console.log("- 'Add Index' butonlarına tıkla");