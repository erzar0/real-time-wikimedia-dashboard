import { LineChart } from "@mui/x-charts/LineChart";
import MessageProps from "../../types/MessageProps";

const Graph: React.FC<MessageProps> = ({ messages, timestamps }) => {
  return (
    <LineChart
      xAxis={[{ data: timestamps }]}
      series={[
        {
          data: messages.map((val) => parseFloat(val)),
          area: true,
        },
      ]}
      width={500}
      height={300}
    />
  );
};

export default Graph;
