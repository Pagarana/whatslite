const express = require('express');
const { createServer } = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const app = express();
app.use(cors());

const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// Store connected users
const connectedUsers = new Map();

// Store chat rooms
const chatRooms = new Map();

io.on('connection', (socket) => {
  console.log('User connected:', socket.id);

  // User joins with nickname
  socket.on('join', (data) => {
    const { nickname, language } = data;
    
    // Store user info
    connectedUsers.set(socket.id, {
      nickname,
      language,
      socketId: socket.id,
      isOnline: true,
      lastSeen: Date.now()
    });

    socket.nickname = nickname;
    
    console.log(`${nickname} joined the chat`);

    // Send updated user list to all clients
    broadcastUserList();

    // Send welcome message
    socket.emit('joined', {
      message: 'Successfully connected to chat server',
      nickname: nickname
    });
  });

  // Handle private messages
  socket.on('private_message', (data) => {
    const { to, message, chatRoomId } = data;
    const sender = connectedUsers.get(socket.id);
    
    if (!sender) return;

    // Find recipient
    const recipient = Array.from(connectedUsers.values())
      .find(user => user.nickname === to);

    if (recipient) {
      // Send message to recipient
      io.to(recipient.socketId).emit('receive_message', {
        from: sender.nickname,
        message: message,
        chatRoomId: chatRoomId,
        timestamp: Date.now(),
        senderLanguage: sender.language
      });

      // Send confirmation to sender
      socket.emit('message_sent', {
        to: to,
        message: message,
        chatRoomId: chatRoomId,
        timestamp: Date.now()
      });

      console.log(`Message from ${sender.nickname} to ${to}: ${message}`);
    } else {
      // Recipient not online
      socket.emit('user_offline', {
        message: `${to} is not online`
      });
    }
  });

  // Handle disconnect
  socket.on('disconnect', () => {
    if (socket.nickname) {
      console.log(`${socket.nickname} disconnected`);
    }
    
    connectedUsers.delete(socket.id);
    broadcastUserList();
  });

  // Handle user status update
  socket.on('update_status', (data) => {
    const user = connectedUsers.get(socket.id);
    if (user) {
      user.isOnline = data.isOnline;
      user.lastSeen = Date.now();
      broadcastUserList();
    }
  });
});

// Broadcast online users list
function broadcastUserList() {
  const userList = Array.from(connectedUsers.values()).map(user => ({
    nickname: user.nickname,
    language: user.language,
    isOnline: user.isOnline,
    lastSeen: user.lastSeen
  }));

  io.emit('user_list_updated', { users: userList });
}

const PORT = process.env.PORT || 3000;

httpServer.listen(PORT, '0.0.0.0', () => {
  console.log(`ChatTranslator server running on port ${PORT}`);
  console.log(`Server accessible at:`);
  console.log(`- Local: http://localhost:${PORT}`);
  console.log(`- Network: http://192.168.86.95:${PORT}`);
  console.log(`Server time: ${new Date().toLocaleString()}`);
});

// Basic web interface for testing
app.get('/', (req, res) => {
  res.send(`
    <h1>ChatTranslator Server</h1>
    <p>Socket.IO server is running on port ${PORT}</p>
    <p>Connected users: ${connectedUsers.size}</p>
    <p>Server time: ${new Date().toLocaleString()}</p>
  `);
});