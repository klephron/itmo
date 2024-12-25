package ru.itmo.zavar;

public record Address(PostalAddress postalAddress, String street, String house) implements Comparable<Address> {

    @Override
    public int compareTo(Address o) {
        int r;
        r = this.postalAddress().compareTo(o.postalAddress());
        if (r != 0) {
            return r;
        }
        r = this.street().compareTo(o.street());
        if (r != 0) {
            return r;
        }
        r = this.house().compareTo(o.house());
        return r;
    }
}
