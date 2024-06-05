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
