import { TaskPack } from '@websocket/pack';
import PagedTable from './PagedTable';

type Props = {
  data: TaskPack[];
};

export default function TaskTable(props: Props) {
  return (
    <PagedTable
      data={props.data}
      rowsPerPage={10}
      headers={['Id', 'Kind']}
      fields={[(e: TaskPack) => String(e.id), (e: TaskPack) => String(e.kind)]}
    />
  );
}
