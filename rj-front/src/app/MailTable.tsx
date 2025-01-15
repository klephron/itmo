import { MailPack } from '@websocket/pack';
import PagedTable from './PagedTable';

type Props = {
  data: MailPack[];
};

export default function MailTable(props: Props) {
  return (
    <PagedTable
      data={props.data}
      rowsPerPage={10}
      headers={[
        'Id',
        'Sender Id',
        'Receiver Id',
        'Value',
        'Status',
        'Created At',
        'Applied At',
        'Sent At',
        'Received At',
      ]}
      fields={[
        (e: MailPack) => String(e.id),
        (e: MailPack) => String(e.sender_actor_id),
        (e: MailPack) => String(e.receiver_actor_id),
        (e: MailPack) => String(e.value),
        (e: MailPack) => String(e.status),
        (e: MailPack) => (e.created_at == null ? '-' : new Date(e.created_at).toISOString()),
        (e: MailPack) => (e.applied_at == null ? '-' : new Date(e.applied_at).toISOString()),
        (e: MailPack) => (e.sent_at == null ? '-' : new Date(e.sent_at).toISOString()),
        (e: MailPack) => (e.received_at == null ? '-' : new Date(e.received_at).toISOString()),
      ]}
    />
  );
}
