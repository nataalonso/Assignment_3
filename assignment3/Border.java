class Border {
    private String country;
    private int length;

    public Border(String country, int length) {
        this.country = country;
        this.length = length;
    }

    // Getter for country
    public String getCountry() {
        return country;
    }

    // Setter for country
    public void setCountry(String country) {
        this.country = country;
    }

    // Getter for length
    public int getLength() {
        return length;
    }

    // Setter for length
    public void setLength(int length) {
        this.length = length;
    }
}
