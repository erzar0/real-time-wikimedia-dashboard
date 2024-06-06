/**
 * Graph Component
 *
 * The `Graph` component is a React functional component used to display a line chart using the MUI X Charts library.
 * It takes in message data and corresponding timestamps to generate a graphical representation of data over time.
 *
 * Props:
 * - `messages`: An array of numerical values representing the data points to be plotted on the y-axis of the line chart.
 * - `timestamps`: An array of timestamps corresponding to each data point in the `messages` array.
 * - `title`: A string representing the title of the graph.
 * - `xAxisLabel`: A string representing the label for the x-axis of the graph.
 * - `yAxisLabel`: A string representing the label for the y-axis of the graph.
 *
 **/

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
