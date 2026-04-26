# Producer/Consumer Simulation System

A graphical queuing network simulation program developed as an object-oriented solution for an assembly line production model. This project simulates processing machines (Ms) and queues (Qs) where products are processed through multiple stages in a concurrent environment.

## 🚀 Features

- **Interactive UI**: Graphically add Queues and Machines and connect them arbitrarily.
- **Real-time Simulation**: Visualize product movement and queue sizes in real-time.
- **Concurrent Processing**: Each machine operates on its own dedicated thread.
- **Dynamic Visualization**: Machines flash upon completion, and products/machines change colors dynamically to reflect the current state.
- **Replay System**: Save a simulation "snapshot" and replay it exactly as it happened.
- **Randomized Rates**: Support for random product arrival rates and random machine service times.

## 🛠️ Technology Stack

- **Backend**: Java 17, Spring Boot
- **Frontend**: Angular
- **Communication**: REST API & Server-Sent Events (SSE) for real-time updates.

## 🏗️ Design Patterns Applied

### 1. Concurrency Pattern
Each `Machine` implements the `Runnable` interface and runs on a separate thread. This ensures that the simulation accurately reflects a real-world production line where machines operate independently. Thread safety is managed using synchronized blocks and thread-safe data structures for queue operations.

### 2. Observer Pattern
Machines act as observers to their input queues. When a queue is empty, the machine registers itself and waits. Once a product is added to the queue, the machine is notified to resume processing. This minimizes CPU polling and ensures efficient resource usage.

### 3. Snapshot (Memento) Pattern
The simulation state (machine states, colors, queue sizes, product locations) is captured at regular intervals using the Snapshot pattern. This allows the system to store the history of a simulation run, which can then be replayed by the user after the simulation ends.

## 📋 Prerequisites

- **Java 17** or higher
- **Node.js** (v18+) and **npm**
- **Maven**

## 🏃 How to Run

### Backend (Spring Boot)
1. Navigate to the `backend` directory.
2. Run the application using Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend will start on `http://localhost:8080`.

### Frontend (Angular)
1. Navigate to the `frontend` directory.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm start
   ```
   Open `http://localhost:4200` in your browser.

## 📖 User Guide

1. **Building the Network**: Use the toolbar to add Machines and Queues.
2. **Connecting**: Click and drag from a component to another to create connections.
3. **Starting Simulation**: Click the 'Start' button. Products will begin arriving at the entry queue (Q0).
4. **Monitoring**: Watch the queues fill up and the machines change color to match the product they are processing.
5. **Replaying**: Once the simulation is stopped, use the 'Replay' button to watch the previous run again.

## 📐 UML Class Diagram
1. DTOs:
<img width="1130" height="1087" alt="Screenshot 2026-04-26 223654" src="https://github.com/user-attachments/assets/985386d8-cb16-4394-b1cd-ea8fc2be6730" />
3. Modles:
<img width="908" height="709" alt="Screenshot 2026-04-26 223720" src="https://github.com/user-attachments/assets/081242bd-b12f-4e4a-ad49-b27221bae357" />
5. Services:
<img width="1220" height="1071" alt="Screenshot 2026-04-26 223742" src="https://github.com/user-attachments/assets/fe5f5532-e44d-4d14-b8d7-b91984f61133" />

[Detailed Class Diagram](https://drive.google.com/file/d/1AImB8NYK-LHp7NqicVeNVx46AQpxJsMS/view)

## 📝 Design Decisions
- **Shortest Queue Routing**: If a machine has multiple output queues, it automatically routes the finished product to the shortest one to balance the load.
- **Time-accurate Replay**: The replay system calculates the exact time difference between captured snapshots to ensure the replay speed matches the original simulation's pace.
- **SSE for Live Updates**: We used Server-Sent Events to push simulation state changes from the backend to the frontend, ensuring the UI remains responsive and synchronized with the multithreaded backend.
- **Reactive State Management**: The frontend uses reactive programming (RxJS) to handle the stream of simulation data from the backend.

## Demo Video:
