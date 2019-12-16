public class newTest {
    public static void main(String[] args) {
        String tt = ".section .data\n" +
                "printf_format:\n" +
                "\t.string \"%d\\n\"\n" +
                ".section .text\n" +
                ".global main\n" +
                "main :\n" +
                "\tpush \t%rbp\n" +
                "\tmov \t%rsp,%rbp\n" +
                "\tsub \t$0x10,%rsp\n" +
                "\tmovl \t$0x04,-0x0c(%rbp)\n" +
                "\tmovl \t$0x02,-0x08(%rbp)\n" +
                "\tmovl \t$0x05,-0x04(%rbp)\n" +
                "\tmov \t-0x04(%rbp), %edx \n" +
                "\tmov \t-0x0c(%rbp), %eax \n" +
                "\tCDQ\n" +
                "\tidiv \t%ebx \n" +
                "\tmov \t%edx,-0x08(%rbp)\n" +
                "\tmovq \t$printf_format,%rdi\n" +
                "\tmovq \t-0x08(%rbp),%rsi\n" +
                "\tmovq \t$0x0,%rax\n" +
                "\tcall \tprintf\n" +
                "\tmov \t-0x04(%rbp), %eax \n" +
                "\tmov \t-0x0c(%rbp), %edx \n" +
                "\tCDQ\n" +
                "\tidiv \t%ebx \n" +
                "\tmov \t%edx,-0x08(%rbp)\n" +
                "\tmovq \t$printf_format,%rdi\n" +
                "\tmovq \t-0x08(%rbp),%rsi\n" +
                "\tmovq \t$0x0,%rax\n" +
                "\tcall \tprintf\n" +
                "\tmov \t-0x04(%rbp),%eax\n" +
                "\tmov \t$0x02,%ebx\n" +
                "\tCDQ\n" +
                "\tidiv \t%ebx\n" +
                "\tmov \t%edx,-0x08(%rbp)\n" +
                "\tmovq \t$printf_format,%rdi\n" +
                "\tmovq \t-0x08(%rbp),%rsi\n" +
                "\tmovq \t$0x0,%rax\n" +
                "\tcall \tprintf\n" +
                "\tmov \t-0x04(%rbp),%eax\n" +
                "\tmov \t$0x01,%ebx\n" +
                "\tCDQ\n" +
                "\tidiv \t%ebx\n" +
                "\tmov \t%edx,-0x08(%rbp)\n" +
                "\tmovq \t$printf_format,%rdi\n" +
                "\tmovq \t-0x08(%rbp),%rsi\n" +
                "\tmovq \t$0x0,%rax\n" +
                "\tcall \tprintf\n" +
                "\tmov \t$0x07,%eax \n" +
                "\tmov \t-0x04(%rbp),%ebx \n" +
                "\tCDQ\n" +
                "\tidiv \t%ebx\n" +
                "\tmov \t%edx,-0x08(%rbp)\n" +
                "\tmovq \t$printf_format,%rdi\n" +
                "\tmovq \t-0x08(%rbp),%rsi\n" +
                "\tmovq \t$0x0,%rax\n" +
                "\tcall \tprintf\n" +
                "\tmov \t$0x11,%eax \n" +
                "\tmov \t-0x04(%rbp),%ebx \n" +
                "\tCDQ\n" +
                "\tidiv \t%ebx\n" +
                "\tmov \t%edx,-0x08(%rbp)\n" +
                "\tmovq \t$printf_format,%rdi\n" +
                "\tmovq \t-0x08(%rbp),%rsi\n" +
                "\tmovq \t$0x0,%rax\n" +
                "\tcall \tprintf\n" +
                "\tmov \t$0x0,%eax\n" +
                "\tleaveq\n" +
                "\tretq\n";
    }
}
