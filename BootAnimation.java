public class BootAnimation {
    private String fakeCommandsTxt;
    private String OSName;

    public BootAnimation() {
        this.fakeCommandsTxt = "Press F1 for Help, F12 Boot for boot Menu or Del for Setup";
        this.OSName = " ____ ___        ________             .__ \n" +
                "|    |   \\______/  _____/ __ _________|__|\n" +
                "|    |   /  ___/   \\  ___|  |  \\_  __ \\  |\n" +
                "|    |  /\\___ \\\\    \\_\\  \\  |  /|  | \\/  |\n" +
                "|______//____  >\\______  /____/ |__|  |__|\n" +
                "             \\/        \\/                 ";

    }

    public void load() throws InterruptedException {
        System.out.println(OSName);
        System.out.println(fakeCommandsTxt);

        System.out.print("Loading");
        for(int i=0; i<12; i++) {
            System.out.print(".");
            Thread.sleep(300);
        }
    }
}