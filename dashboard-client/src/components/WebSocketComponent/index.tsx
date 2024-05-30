import React, { useEffect } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

interface WebSocketComponentProps {
  url: string;
  topic: string;
}

const WebSocketComponent: React.FC<WebSocketComponentProps> = ({
  url,
  topic,
}) => {
  useEffect(() => {
    const socket = new SockJS(url);

    const stompClient = new Client({
      webSocketFactory: () => socket,
      debug: (str) => {
        console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (frame) => {
      console.log("Connected:", frame);
      stompClient.subscribe(topic, (message) => {
        console.log("Message received:", message.body);
      });
    };

    stompClient.onDisconnect = (frame) => {
      console.log("Disconnected:", frame);
    };

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, [url, topic]);

  return (
    <div>
      <p>WebSocket connection to {url}</p>
      <p>Subscribed to topic: {topic}</p>
    </div>
  );
};

export default WebSocketComponent;
