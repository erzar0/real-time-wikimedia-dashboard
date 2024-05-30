import React, { useEffect, useState } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import MessageProps from "../../types/MessageProps";

interface WebSocketComponentProps {
  url: string;
  topic: string;
  keepMessagesCount: number;
  messageProcessor: React.FC<MessageProps>;
}

const WebSocketComponent: React.FC<WebSocketComponentProps> = ({
  url,
  topic,
  keepMessagesCount,
  messageProcessor: MessageProcessor,
}) => {
  const [messages, setMessages] = useState<string[]>([]);
  const [timestamps, setTimestamps] = useState<Date[]>([]);

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
        setMessages((prevMessages) => {
          const newMessages = [...prevMessages, message.body];
          if (newMessages.length > keepMessagesCount) {
            return newMessages.slice(1);
          }
          return newMessages;
        });
        setTimestamps((prevTimestamps) => {
          const newTimestamps = [...prevTimestamps, new Date()];
          if (newTimestamps.length > keepMessagesCount) {
            return newTimestamps.slice(1);
          }
          return newTimestamps;
        });
      });
    };

    stompClient.onDisconnect = (frame) => {
      console.log("Disconnected:", frame);
    };

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, [url, topic, keepMessagesCount]);

  return (
    <div>
      <p>WebSocket connection to {url}</p>
      <p>Subscribed to topic: {topic}</p>
      <MessageProcessor timestamps={timestamps} messages={messages} />
    </div>
  );
};

export default WebSocketComponent;
