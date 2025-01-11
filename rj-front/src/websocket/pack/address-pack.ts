import { PostalAddressPack } from './postal-address-pack';

export interface AddressPack extends PostalAddressPack {
  street: string;
  house: string;
}
