# 🚀 P2P Distributed Chat System

A lightweight **peer-to-peer** chat application where every user acts as both client and server. No central server required!

The source code includes:
- Node.java: Handles connections, sending/receiving messages, local message history, and threads for listening to incoming messages.
- Main.java: Provides a console-based user interface and interacts with the Node object.

## 📌 Overview
This project implements a **pure P2P chat system** using Java sockets. Each node in the network can:
- Connect directly to multiple peers
- Send/receive messages in real-time  
- Maintain local chat history
- Manage connected peers list

## 🎯 Features
- ✅ **Decentralized**: No central server needed
- ✅ **Direct Connections**: Messages go straight between users
- ✅ **Real-time Chat**: Instant message delivery
- ✅ **Multiple Peers**: Chat with many friends simultaneously
- ✅ **Local History**: All conversations saved on your computer
- ✅ **Simple Commands**: Easy-to-use command interface

## 🏗 Architecture
 
  +       ┌─────────┐      ┌─────────┐      ┌─────────┐
          │ Node    │◄────►│ Node    │◄────►│ Node    │
          │ (Alice) │      │ (Bob)   │      │(Charlie)│
          └─────────┘      └─────────┘      └─────────┘
             ▲                   ▲               ▲
             │                   │               │
           Server             Server           Server 
              +                   +               +
           Client             Client            Client

## ⚡ How to Run
1. Clone the repository: `git clone https://github.com/31773207/p2pchat-system.git`
2. Navigate to `src` folder: `cd src`
3. Compile the code: `javac Main.java Node.java`
4. Run a node: `java Main`
5. Follow the prompts to enter your username and port number
6. Use `/connect <IP>:<PORT>` to connect to peers

## 📝 Commands
| Command               | Description                         |
|----------------------|--------------------------------------|
| /connect <IP>:<PORT> | Connect to a peer                    |
| /list                | Show connected peers                 |
| /history             | Show message history                 |
| /clearhistory        | Clear message history                |
| /savehistory <file>  | Save chat history to a file          |
| /quit                | Exit the chat                        |
---------------------------------------------------------------

