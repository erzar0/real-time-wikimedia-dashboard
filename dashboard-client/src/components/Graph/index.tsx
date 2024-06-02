import { LineChart } from "@mui/x-charts/LineChart";
import MessageProps from "../../types/MessageProps";

const Graph: React.FC<MessageProps> = ({
  messages,
  timestamps,
  title,
  xAxisLabel,
  yAxisLabel,
}) => {
  return (
    <div className="widget">
      <h2>{title}</h2>
      <LineChart
        title={title}
        series={[
          {
            data: messages.map((val) => parseFloat(val)),
            area: true,
            label: yAxisLabel,
          },
        ]}
      />
    </div>
  );
};

export default Graph;
