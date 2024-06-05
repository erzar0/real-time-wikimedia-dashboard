import React, { useEffect, useState } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import MessageProps from "../../types/MessageProps";
import ChartConfigProps from "../../types/ChartConfigProps";

interface WebSocketComponentProps {
  url: string;
  topic: string;
  keepMessagesCount: number;
  messageProcessor: React.FC<MessageProps>;
  chartConfig: ChartConfigProps;
}

const WebSocketComponent: React.FC<WebSocketComponentProps> = ({
  url,
  topic,
  keepMessagesCount,
  messageProcessor: MessageProcessor,
  chartConfig,
}) => {
  const [messages, setMessages] = useState<string[]>([]);
  const [timestamps, setTimestamps] = useState<string[]>([]);

  useEffect(() => {
    const socket = new SockJS(url);

    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (frame) => {
      stompClient.subscribe(topic, (message) => {
        if (
          message.body &&
          message.body !== "" &&
          message.command === "MESSAGE"
        ) {
          setMessages((prevMessages) => {
            const newMessages = [...prevMessages, message.body];
            if (newMessages.length > keepMessagesCount) {
              return newMessages.slice(1);
            }
            return newMessages;
          });
          setTimestamps((prevTimestamps) => {
            const newTimestamps =
              prevTimestamps.length > 0
                ? [...prevTimestamps, new Date().getUTCSeconds().toString()]
                : [new Date().getUTCSeconds().toString()];
            if (newTimestamps.length > keepMessagesCount) {
              return newTimestamps.slice(1);
            }
            return newTimestamps;
          });
        }
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
    <MessageProcessor
      timestamps={timestamps}
      messages={messages}
      {...chartConfig}
    />
  );
};

export default WebSocketComponent;
