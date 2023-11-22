package ASM;

class Comment extends AsmDirective {
    public Comment(String s) {
        super(String.format(";%-5s %-20s","",s));
    }
}
