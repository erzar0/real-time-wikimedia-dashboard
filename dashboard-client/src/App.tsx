import React from "react";
import logo from "./logo.svg";
import "./App.css";
import Header from "./components/Header";
import Footer from "./components/Footer";
import WebSocketComponent from "./components/WebSocketComponent";

const WS_URL = "http://localhost:8081/ws";
const TOPIC = "/topic/recentchange-event-count";

function App() {
  return (
    <div className="App">
      <Header title="Real-Time Dashboard" />
      <WebSocketComponent url={WS_URL} topic={TOPIC} />
      <Footer company="Eryk Zarębski" year={2024} />
    </div>
  );
}

export default App;
