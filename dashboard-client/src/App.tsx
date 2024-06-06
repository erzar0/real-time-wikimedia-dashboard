/**
 * App Component
 *
 * The `App` component is the main component of the real-time dashboard application. It integrates various components to display real-time data visualizations.
 **/

import React from "react";
import Header from "./components/Header";
import WebSocketComponent from "./components/WebSocketComponent";
import Graph from "./components/Graph";
import RankingCharts from "./components/RankingCharts";

const WS_URL = "http://localhost:8081/ws";
const RECENTCHANGE_EVENT_COUNT_TOPIC = "/topic/recentchange-event-count";
const RECENTCHANGE_LENGTH_CHANGE_TOPIC = "/topic/recentchange-length-change";
const RECENTCHANGE_ACTIVE_USERS_TOPIC = "/topic/recentchange-active-users";
const KEEP_MESSAGES_COUNT_GRAPH = 100;
const KEEP_MESSAGES_COUNT_RANKINGS_CHART = 1;

function App() {
  return (
    <div className="container">
      <Header title="Real-Time Dashboard" />
      <main>
        <WebSocketComponent
          url={WS_URL}
          topic={RECENTCHANGE_EVENT_COUNT_TOPIC}
          keepMessagesCount={KEEP_MESSAGES_COUNT_GRAPH}
          messageProcessor={Graph}
          chartConfig={{
            title: "Event count",
            xAxisLabel: "count",
            yAxisLabel: "time",
          }}
        />
        <WebSocketComponent
          url={WS_URL}
          topic={RECENTCHANGE_LENGTH_CHANGE_TOPIC}
          keepMessagesCount={KEEP_MESSAGES_COUNT_GRAPH}
          messageProcessor={Graph}
          chartConfig={{
            title: "Length change",
            xAxisLabel: "count",
            yAxisLabel: "time",
          }}
        />
        <WebSocketComponent
          url={WS_URL}
          topic={RECENTCHANGE_ACTIVE_USERS_TOPIC}
          keepMessagesCount={KEEP_MESSAGES_COUNT_RANKINGS_CHART}
          messageProcessor={RankingCharts}
          chartConfig={{
            title: "Active users",
            xAxisLabel: "count",
            yAxisLabel: "username",
          }}
        />
      </main>
    </div>
  );
}

export default App;
