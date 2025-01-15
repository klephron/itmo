import { PostalAddressPack } from '@websocket/pack';
import PagedTable from './PagedTable';

type Props = {
  data: PostalAddressPack[];
};

export default function PostalAddressTable(props: Props) {
  return (
    <PagedTable
      data={props.data}
      rowsPerPage={10}
      headers={['Country', 'City', 'Postal Code']}
      fields={[
        (e: PostalAddressPack) => String(e.country),
        (e: PostalAddressPack) => String(e.city),
        (e: PostalAddressPack) => String(e.postal_code),
      ]}
    />
  );
}
