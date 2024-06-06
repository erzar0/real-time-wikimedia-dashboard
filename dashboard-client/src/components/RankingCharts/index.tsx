/**
 * RankingCharts Component
 *
 * The `RankingCharts` component is a React functional component used to display multiple bar charts based on the provided data.
 *
 * Props:
 * - `messages`: An array of JSON strings containing data for generating the bar charts.
 * - `timestamps`: Not used in this component.
 * - `title`: A string representing the title of the charts.
 * - `xAxisLabel`: A string representing the label for the x-axis of the charts.
 * - `yAxisLabel`: Not used in this component.
 **/

import React from "react";
import { BarChart } from "@mui/x-charts/BarChart";
import MessageProps from "../../types/MessageProps";

const RankingCharts: React.FC<MessageProps> = ({
  messages,
  timestamps,
  title,
  xAxisLabel,
  yAxisLabel,
}) => {
  if (messages.length < 1) {
    return <></>;
  }
  const data = JSON.parse(messages[0]);

  const chartData = (dataKey: string) =>
    data[dataKey].map((item: any) => ({
      username: item.username,
      changesLength: item.changesLength,
    }));

  return (
    <ul>
      {Object.keys(data).map((key) => (
        <li className="widget" key={key}>
          <h2>{key}</h2>
          <div style={{ width: "100%", height: "500px", marginBottom: "40px" }}>
            <BarChart
              dataset={chartData(key)}
              yAxis={[{ scaleType: "band", dataKey: "username" }]}
              series={[
                {
                  dataKey: "changesLength",
                  label: xAxisLabel,
                },
              ]}
              layout="horizontal"
              title={title}
            />
          </div>
        </li>
      ))}
    </ul>
  );
};

export default RankingCharts;
