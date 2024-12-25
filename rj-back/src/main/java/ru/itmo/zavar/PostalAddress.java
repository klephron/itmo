package ru.itmo.zavar;

public record PostalAddress(String country, String city, String postalCode) implements Comparable<PostalAddress> {

    @Override
    public int compareTo(PostalAddress o) {
        int r;
        r = this.country().compareTo(o.country());
        if (r != 0) {
            return r;
        }
        r = this.city().compareTo(o.city());
        if (r != 0) {
            return r;
        }
        r = this.postalCode().compareTo(o.postalCode());
        return r;
    }

}
