import { LineChart } from "@mui/x-charts/LineChart";
import MessageProps from "../../types/MessageProps";

const Graph: React.FC<MessageProps> = ({
  messages,
  timestamps,
  title,
  xAxisLabel,
  yAxisLabel,
}) => {
  const dataset = messages.map((message, idx) => {
    return {
      message: parseFloat(message),
      timestamp: parseFloat(timestamps[idx]) - new Date().getSeconds(),
    };
  });
  console.log(dataset);

  return (
    <div className="widget">
      <h2>{title}</h2>
      <LineChart
        title={title}
        dataset={dataset}
        series={[
          {
            dataKey: "message",
            label: xAxisLabel,
            area: true,
          },
        ]}
      />
    </div>
  );
};

export default Graph;
