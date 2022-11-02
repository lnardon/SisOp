// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;


public class Sistema {


    // -------------------------------------------------------------------------------------------------------
    // --------------------- H A R D W A R E - definicoes de HW ----------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // --------------------- M E M O R I A -  definicoes de opcode e palavra de memoria ----------------------

    public class Word {    // cada posicao da memoria tem uma instrucao (ou um dado)
        public Opcode opc;    //
        public int r1;        // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
        public int r2;        // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
        public int p;        // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

        public Word(Opcode _opc, int _r1, int _r2, int _p) {
            opc = _opc;
            r1 = _r1;
            r2 = _r2;
            p = _p;
        }
    }
    // -------------------------------------------------------------------------------------------------------
    // --------------------- C P U  -  definicoes da CPU -----------------------------------------------------

    public enum Opcode {
        DATA, ___,            // se memoria nesta posicao tem um dado, usa DATA, se não usada é NULO ___
        JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, STOP,   // desvios e parada
        ADDI, SUBI, ADD, SUB, MULT,             // matemáticos
        LDI, LDD, STD, LDX, STX, SWAP,          // movimentação
        TRAP;                                   //
    }

    public enum Interrupts {
        INT_NONE,
        INT_INVALID_INSTRUCTION,    // Nunca será usada, pois o Java não deixará compilar
        INT_INVALID_ADDRESS,        // Nossa memória tem 1024 posições
        INT_OVERFLOW,               // Nossa memória só trabalha com inteiros, ou seja de -2,147,483,648 até 2,147,483,647
        INT_SYSTEM_CALL;            // Ativa chamada de I/O pelo comando TRAP
    }

    public class CPU {
        // característica do processador: contexto da CPU ...
        private int pc;
        private Word ir;
        private int[] reg;
        public int maxInt;
        private int[] paginasAlocadas;
        private int[] tabelaDePaginas;
        private int tamPaginaMemoria;

        public Interrupts interrupts;

        private Word[] m;

        public CPU(Word[] _m, int tamPaginaMemoria, int maxInt) {
            m = _m;
            reg = new int[10];
            this.maxInt = maxInt;          // números aceitos -100_000 até 100_000
            this.tamPaginaMemoria = tamPaginaMemoria;
        }

        public void setContext(int _pc, int [] paginasAlocadas) {
            pc = _pc;
            this.interrupts = Interrupts.INT_NONE;
            this.paginasAlocadas = paginasAlocadas;
        }

        private void dump(Word w) {
            System.out.print("[ ");
            System.out.print(w.opc);
            System.out.print(", ");
            System.out.print(w.r1);
            System.out.print(", ");
            System.out.print(w.r2);
            System.out.print(", ");
            System.out.print(w.p);
            System.out.println("  ] ");
        }

        private void showState() {
            System.out.println("       " + pc);
            System.out.print("           ");
            for (int i = 0; i < reg.length; i++) {
                System.out.print("r" + i);
                System.out.print(": " + reg[i] + "     ");
            }
            ;
            System.out.println("");
            System.out.print("           ");
            dump(ir);
        }


        private boolean isRegisterValid(int register) {
            if (register < 0 || register >= reg.length) {
                interrupts = Interrupts.INT_INVALID_INSTRUCTION;
                return false;
            }
            return true;
        }

        private boolean isAddressValid(int address) {
            if (address < 0 || address >= m.length) {
                interrupts = Interrupts.INT_INVALID_ADDRESS;
                return false;
            }
            return true;
        }

        private boolean isNumberValid(int number) {
            if (number < maxInt * -1 || number > maxInt) {
                interrupts = Interrupts.INT_OVERFLOW;
                return false;
            }
            return true;
        }

        public int traduzEndereco (int endereco){
            try {
                return (paginasAlocadas[(endereco / tamPaginaMemoria)] * tamPaginaMemoria) + (endereco % tamPaginaMemoria);

            } catch(ArrayIndexOutOfBoundsException e) {
                return -1;
            }
        }

        public void run() {
            boolean run = true;
            while (run) {

                ir = m[traduzEndereco(pc)];
                switch (ir.opc) {

                    case LDI: // Rd ← k
                        if (isRegisterValid(ir.r1) && isNumberValid(ir.p)) {
                            reg[ir.r1] = ir.p;
                            pc++;
                            break;
                        } else
                            break;

                    case LDD: // Rd ← [A]
                        if (isRegisterValid(ir.r1) && isAddressValid(traduzEndereco(ir.p)) && isNumberValid(m[ir.p].p)) {
                            reg[ir.r1] = m[traduzEndereco(ir.p)].p;
                            pc++;
                            break;
                        } else
                            break;

                    case STD: // [A] ← Rs
                        if (isRegisterValid(ir.r1) && isAddressValid(traduzEndereco(ir.p)) && isNumberValid(reg[ir.r1])) {
                            m[traduzEndereco(ir.p)].opc = Opcode.DATA;
                            m[traduzEndereco(ir.p)].p = reg[ir.r1];
                            pc++;
                            break;
                        } else
                            break;

                    case ADD: // Rd ← Rd + Rs
                        if (isRegisterValid(ir.r2) && isRegisterValid(ir.r1) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r2]) && isNumberValid(reg[ir.r1] + reg[ir.r2])) {
                            reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }

                    case MULT: // Rd ← Rd * Rs
                        if (isRegisterValid(ir.r2) && isRegisterValid(ir.r1)) {
                            if (isNumberValid(reg[ir.r1] * reg[ir.r2]) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r2])) {
                                reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
                                pc++;
                                break;
                            } else {
                                pc++;
                                break;
                            }
                        } else
                            break;

                    case ADDI: // Rd ← Rd + k
                        if (isRegisterValid(ir.r1) && isNumberValid(reg[ir.r1]) && isNumberValid(ir.p) && isNumberValid(reg[ir.r1] + ir.p)) {
                            reg[ir.r1] = reg[ir.r1] + ir.p;
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }


                    case STX: // [Rd] ←Rs
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            m[traduzEndereco(reg[ir.r1])].opc = Opcode.DATA;
                            m[traduzEndereco(reg[ir.r1])].p = reg[ir.r2];
                            pc++;
                            break;
                        } else
                            break;

                    case LDX: // Rd ← [Rs]
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r2])) && isNumberValid(m[reg[ir.r2]].p)) {
                            reg[ir.r1] = m[traduzEndereco(reg[ir.r2])].p;
                            pc++;
                            break;
                        } else
                            break;

                    case SUB: // Rd ← Rd - Rs
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isNumberValid(reg[ir.r2]) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r1] - reg[ir.r2])) {
                            reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }

                    case SUBI: // Rd ← Rd - k
                        if (isRegisterValid(ir.r1) && isNumberValid(reg[ir.r1]) && isNumberValid(ir.p) && isNumberValid(reg[ir.r1] - ir.p)) {
                            reg[ir.r1] = reg[ir.r1] - ir.p;
                            pc++;
                            break;
                        } else {
                            interrupts = Interrupts.INT_OVERFLOW;
                            pc++;
                            break;
                        }

                    case JMP: //  PC ← k
                        if (isAddressValid(traduzEndereco(ir.p))) {
                            pc = ir.p;
                            break;
                        } else
                            break;

                    case JMPI: //  PC ← Rs
                        if (isRegisterValid(traduzEndereco(ir.r1)) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            pc = reg[ir.r1];
                            break;
                        } else
                            break;


                    case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isRegisterValid(ir.r1) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            if (reg[ir.r2] > 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIGM: // If Rc > 0 Then PC ← [A] Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(ir.p)) && isAddressValid(traduzEndereco(m[ir.p].p))) {
                            if (reg[ir.r2] > 0) {
                                pc = m[traduzEndereco(ir.p)].p;
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPILM: // If Rc < 0 Then PC ← [A] Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(ir.p)) && isAddressValid(traduzEndereco(m[ir.p].p))) {
                            if (reg[ir.r2] < 0) {
                                pc = m[traduzEndereco(ir.p)].p;
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIEM: // If Rc = 0 Then PC ← [A] Else PC ← PC +1
                        if (isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(ir.p)) && isAddressValid(traduzEndereco(m[ir.p].p))) {
                            if (reg[ir.r2] == 0) {
                                pc = m[traduzEndereco(ir.p)].p;
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;


                    case JMPIE: // If Rc = 0 Then PC ← Rs Else PC ← PC +1
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            if (reg[ir.r2] == 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIL: //  PC ← Rs
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isAddressValid(traduzEndereco(reg[ir.r1]))) {
                            if (reg[ir.r2] < 0) {
                                pc = reg[ir.r1];
                            } else {
                                pc++;
                            }
                            break;
                        } else
                            break;

                    case JMPIM: //  PC ← [A]
                        if (isAddressValid(traduzEndereco(m[ir.p].p)) && isAddressValid(traduzEndereco(ir.p))) {
                            pc = m[traduzEndereco(ir.p)].p;
                            break;
                        } else
                            break;

                    case SWAP: // t <- r1; r1 <- r2; r2 <- t
                        if (isRegisterValid(ir.r1) && isRegisterValid(ir.r2) && isNumberValid(reg[ir.r1]) && isNumberValid(reg[ir.r2])) {
                            int temp;
                            temp = reg[ir.r1];
                            reg[ir.r1] = reg[ir.r2];
                            reg[ir.r2] = temp;
                            pc++;
                            break;
                        } else
                            break;

                    case STOP:
                        break;

                    case TRAP:
                        interrupts = Interrupts.INT_SYSTEM_CALL;
                        pc++;
                        break;

                    case DATA:
                        pc++;
                        break;

                    default:
                        interrupts = Interrupts.INT_INVALID_INSTRUCTION;
                }

                
                if (ir.opc == Opcode.STOP) {
                    break;
                }
            }
        }
    }
    // ------------------ C P U - fim ------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------


    // ------------------- V M  - constituida de CPU e MEMORIA -----------------------------------------------
    // -------------------------- atributos e construcao da VM -----------------------------------------------
    public class VM {
        public int tamMem;
        public Word[] m;
        public CPU cpu;
        private int tamanhoPaginaMemoria;

        public VM(int tamMem, int tamanhoPaginaMemoria, int maxInt) {
            this.tamMem = tamMem;
            this.tamanhoPaginaMemoria = tamanhoPaginaMemoria;
            m = new Word[tamMem];
            for (int i = 0; i < tamMem; i++) {
                m[i] = new Word(Opcode.___, -1, -1, -1);
            }
            ;

            cpu = new CPU(m, tamanhoPaginaMemoria, maxInt);
        }

        public int getTamMem() {
            return tamMem;
        }
    }
    // ------------------- V M  - fim ------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // --------------------H A R D W A R E - fim -------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    // ------------------- S O F T W A R E - inicio ----------------------------------------------------------

    public class GerenciadorMemoria {

        private Word[] mem;
        private int tamPagina;
        private int tamFrame;
        private int nroFrames;
        private boolean[] tabelaPaginas;
        public int [] framesAlocados;

        public GerenciadorMemoria(Word[] mem, int tamPagina) {
            this.mem = mem;
            this.tamPagina = tamPagina;
            tamFrame = tamPagina;
            nroFrames = mem.length / tamPagina;
            tabelaPaginas = initFrames();
            
        }

        private boolean[] initFrames() {
            boolean[] free = new boolean[nroFrames];
            for (int i = 0; i < nroFrames; i++) {
                free[i] = true;
            }
            return free;
        }

        public void dump(Word w) {
            System.out.print("[ ");
            System.out.print(w.opc);
            System.out.print(", ");
            System.out.print(w.r1);
            System.out.print(", ");
            System.out.print(w.r2);
            System.out.print(", ");
            System.out.print(w.p);
            System.out.println("  ] ");
        }

        public void dumpMem(Word[] m, int ini, int fim) {
            for (int i = ini; i < fim; i++) {
                System.out.print(i);
                System.out.print(":  ");
                dump(m[i]);
            }
        }

        public int getQuantidadePaginasUsadas(){
            int quantidade = 0;
            for (int i=0; i<tabelaPaginas.length; i++){
                if (tabelaPaginas[i]==false) quantidade++;
            }
            return quantidade;
        }

        public void dumpMemoriaUsada(Word[] m) {
            int fim = getQuantidadePaginasUsadas() * tamPagina;
            for (int i = 0; i < fim; i++) {
                System.out.print(i);
                System.out.print(":  ");
                dump(m[i]);
            }
        }

        public void dumpPagina (Word[]m, int pagina){
            int ini = tamPagina * pagina;
            int fim = ini + tamPagina;
            for (int i = ini; i < fim; i++) {
                System.out.print(i);
                System.out.print(":  ");
                dump(m[i]);
            }
        }


        // retorna null se não conseguir alocar, ou um array com os frames alocadas
        public int[] aloca(Word[] programa) {
            int quantidadePaginas = programa.length / tamPagina;
            if (programa.length % tamPagina > 0) quantidadePaginas++;
            framesAlocados = new int[quantidadePaginas];
            int indiceAlocado = 0;
            int indicePrograma = 0;

            int framesLivres =0;
            for (int i = 0; i < nroFrames; i++) {
                if (tabelaPaginas[i])
                    framesLivres++;
            }

            if (framesLivres <= quantidadePaginas){
                framesAlocados [0] = -1;
                return framesAlocados;
            }

            for (int i = 0; i < nroFrames; i++) {
                if (quantidadePaginas == 0) break;
                if (tabelaPaginas[i]) {
                    tabelaPaginas[i] = false;

                    for (int j = tamPagina * i; j < tamPagina * (i + 1); j++) {
                        if (indicePrograma >= programa.length) break;
                        mem[j].opc = programa[indicePrograma].opc;
                        mem[j].r1 = programa[indicePrograma].r1;
                        mem[j].r2 = programa[indicePrograma].r2;
                        mem[j].p = programa[indicePrograma].p;
                        indicePrograma++;
                    }
                    framesAlocados[indiceAlocado] = i;
                    indiceAlocado++;
                    quantidadePaginas--;
                }

            }

            return framesAlocados;
        }

        public int[] getFramesAlocados(){
            return framesAlocados;
        }

        public boolean[] getTabelaDePaginas(){
            return tabelaPaginas;
        }

        public void desaloca(PCB process){
            int[] paginas = process.getPaginasAlocadas();
            for(int i = 0; i < paginas.length; i ++) {
                tabelaPaginas[paginas[i]] = true;

                for (int j = tamPagina * paginas[i]; j < tamPagina * (paginas[i] + 1); j++) {
                    mem[j].opc = Opcode.___;
                    mem[j].r1 = -1;
                    mem[j].r2 = -1;
                    mem[j].p = -1;
                }
            }
        }

    }

    public class GerenciadorProcessos {
        private GerenciadorMemoria gm;
        private Word[] memory;
        public PCB running;
        private LinkedList<PCB> prontos;
        private int process_id;
        public int tamPagina;
        public CPU cpu;

        public GerenciadorProcessos(GerenciadorMemoria gm, Word[] memory) {
            process_id=0;
            this.gm = gm;
            this.memory = memory;
            this.prontos = new LinkedList<>();
        }

        public PCB getRunning(){
            return running;
        }

        public LinkedList<PCB> getProntos() {
            return prontos;
        }

        public int[] getPaginasAlocadas (int process_id){
            int [] paginasAlocadas = new int[1];
            boolean achou = false;
            for (int i = 0; i < prontos.size(); i++) {
                if (prontos.get(i).id==process_id){
                    paginasAlocadas = prontos.get(i).paginasAlocadas;
                    achou = true;
                }
            }

            if (achou==false){
                paginasAlocadas= new int[1];
                paginasAlocadas[0]=-1;
            }

            return paginasAlocadas;
        }

        public PCB getProcesso (int process_id){
            PCB process = null;
            for (int i = 0; i < prontos.size(); i++) {
                if (prontos.get(i).id==process_id){
                    process = prontos.get(i);
                }
            }
            return process;
        }

        public int criaProcesso(Word [] programa){
            System.out.println("Processo " + process_id + " criado");
            int[] paginasAlocadas = gm.aloca(programa);

            if (paginasAlocadas[0]==-1){
                return -1;
            }

            PCB process = new PCB(process_id, paginasAlocadas, 0, new int[10], new Word(Opcode.___,-1,-1,-1), Interrupts.INT_NONE);
            process_id++;
            prontos.add(process);

            //debug
            System.out.println("Páginas alocadas");
            for (int i=0; i<paginasAlocadas.length; i++){
                System.out.println(paginasAlocadas[i] + " ");
            }

            return process_id-1;
        }


        public void finalizaProcesso(PCB process){
            gm.desaloca(process);
            prontos.remove(process);
        }

        public void listaProcessos(){
            System.out.println("Processo Atual.");
            if  (this.running != null) System.out.println(this.running);
            System.out.println("Processos da fila.");
            if (this.prontos.size() > 0){
                for (PCB process : this.prontos) {
                    System.out.println(process);
                }
            }                
        }

    }


    public class PCB {

        public int id;
        public int programCounter;
        public int[] paginasAlocadas;
        public int[] registradores;
        public Word instructionRegister;
        public Interrupts interrupt;
        public String nomeDoPrograma;

        public PCB(int id, int[]paginasAlocadas, int pc, int [] reg, Word ir, Interrupts interrupt) {
            this.id= id;
            this.paginasAlocadas = paginasAlocadas;
            this.programCounter = pc;
            this.registradores = new int[reg.length];
            this.instructionRegister = ir;
            this.interrupt = interrupt;
        }

        public int[] getPaginasAlocadas(){
            return this.paginasAlocadas;
        }

        public int getId(){
            return this.id;
        }

        public String toString(){
            String paginasAlocadasString = "[";
            for (int i=0; i<paginasAlocadas.length; i++){
                paginasAlocadasString = paginasAlocadasString + " " + paginasAlocadas[i];
            }
            paginasAlocadasString = paginasAlocadasString + "]";
    
            String process = "Process id: " + id + ", Program counter: " + programCounter + ", Páginas alocadas: " + paginasAlocadasString;
            return process;
        }

    }


    // -------------------------------------------------------------------------------------------------------
    // -------------------  S I S T E M A --------------------------------------------------------------------

    public VM vm;
    public static Programas progs;
    public GerenciadorMemoria gm;
    public GerenciadorProcessos gp;

    public Sistema(int tamMemoria, int tamPagina, int maxInt){
        vm = new VM(tamMemoria, tamPagina, maxInt);
        progs = new Programas();
        gm = new GerenciadorMemoria(vm.m, tamPagina);
        gp = new GerenciadorProcessos(gm, vm.m);
    }

    public void roda(Word[] programa){
        if (gp.criaProcesso(programa)==-1){
            System.out.println("Falta memoria para rodar o programa");
            return;
        }

        System.out.println("************************ programa carregado ");
        gm.dumpMemoriaUsada(vm.m);
        System.out.println("************************ após execucao ");
        gm.dumpMemoriaUsada(vm.m);

    }

    

    public int cria(Word[] programa){
        int idDoProcessoCriado;
        idDoProcessoCriado = gp.criaProcesso(programa);
        if (idDoProcessoCriado==-1){
            System.out.println("Falta memoria para rodar o programa");
            return idDoProcessoCriado;
        }

        System.out.println("************************ programa carregado ");
        gm.dumpMemoriaUsada(vm.m);
        return idDoProcessoCriado;
    }

    public void executa(int processId) {
        System.out.println("Iniciando execução do processo");
        int [] paginasAlocadas = gp.getPaginasAlocadas(processId);
        if (paginasAlocadas[0]==-1){
            System.out.println("Processo não existe");
            return;
        }
        System.out.println("Páginas alocadas");
        for (int i=0; i<paginasAlocadas.length; i++){
            System.out.println(paginasAlocadas[i] + " ");
        }
        vm.cpu.setContext(0, paginasAlocadas);
        vm.cpu.run();
        System.out.println("************************ programa executado ");
        gm.dumpMemoriaUsada(vm.m);
    }

    public void dump (int processId){
        System.out.println("************ dump do processo " + processId + "************");
        int [] paginasAlocadas = gp.getPaginasAlocadas(processId);

        for (int i=0; i<paginasAlocadas.length; i++){
            gm.dumpPagina(vm.m, paginasAlocadas[i]);
        }
    }

    public void dumpM (int inicio, int fim){
        System.out.println("************ dump com inicio em " + inicio + " e fim em " + fim + " ************");
        for (int i = inicio; i<=fim; i++){
            gm.dumpPagina(vm.m, i);
        }
    }

    public void desaloca (int processId){
        PCB processo = gp.getProcesso(processId);
        int [] paginasAlocadas = gp.getPaginasAlocadas(processId);
        gp.finalizaProcesso(processo);
        System.out.println("--------------Processo " + processId + " desalocado---------------");
        for (int i=0; i<paginasAlocadas.length; i++){
            gm.dumpPagina(vm.m, paginasAlocadas[i]);
        }

    }

    public void listaProcessos(){
        System.out.println("-------------Processo em execução:");
        if (running==null) System.out.println("Nenhum");
        else System.out.println(running);

        System.out.println("-------------Processos na lista de prontos");
        if (gp.prontos.size()==0) System.out.println("Nenhum");
        else {
            for (PCB process : gp.prontos) {
                System.out.println(process);
            }
        }
    }

    // -------------------  S I S T E M A - fim --------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    public static void main(String args[]) {
        int tamanhoDamemoria = 1024;
        int tamanhoDaPaginadeMemoria = 16;
        int maxInt = 100_000;

        Sistema s = new Sistema(tamanhoDamemoria, tamanhoDaPaginadeMemoria, maxInt);

        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println("");
            System.out.println("********************************************");
            System.out.println("\nDigite um comando. Comandos disponíveis e exemplos de uso:\n\n" +
                    "* cria nomeDePrograma - exemplo: cria fibonacci\n" +
                    "* nomes de programas disponíveis: fibonacci, fatorial, bubble\n" +
                    "* dump id             - exemplo: dump 1\n" +
                    "* desaloca id         - exemplo: des 1\n" +
                    "* dumpM inicio fim    - exemplo: dumpM 2,5\n" +
                    "* executa id          - exemplo: executa 2\n" +
                    "* listaProcessos (não funcional)      - exemplo: lista\n" +
                    "* sair do programa    - exemplo: sair");

            String palavra = in.nextLine();

            if (palavra.equals("lista")){
                s.listaProcessos();
            }
            
            else if (palavra.equals("sair")){
                System.out.println("Até mais!");
                break;
            }

            else if (palavra.contains(" ")){
                String [] input = palavra.split(" ");
                String comando = input[0];
                String arg = input[1];

                if (comando.equals("cria")){
                    if (arg.equals("fibonacci")){
                        s.cria(Sistema.progs.fibonacci10);
                    }
                    else if (arg.equals("fatorial")){
                        s.cria(Sistema.progs.fatorial);
                    }
                    else if (arg.equals("bubble")){
                        s.cria(Sistema.progs.bubbleSort);
                    }
                    else if (arg.equals("fatorialinput")) {
                        s.cria(Sistema.progs.fatorialComInput);
                    }
                    else if (arg.equals("overflow")) {
                        s.cria(Sistema.progs.overflowTest);
                    }
                    else{
                        System.out.println("Programa desconhecido");
                    }
                }

                else if (comando.equals("executa")){
                    int process = Integer.parseInt(arg);
                    s.executa(process);
                }

                else if (comando.equals("dump")){
                    int process = Integer.parseInt(arg);
                    s.dump(process);
                }

                else if (comando.equals("desaloca")){
                    int process = Integer.parseInt(arg);
                    s.desaloca(process);
                }

                else if (comando.equals("dumpM")){
                    String [] numeros = arg.split(",");
                    int inicio = Integer.parseInt(numeros[0]);
                    int fim = Integer.parseInt(numeros[1]);
                    s.dumpM(inicio,fim);
                }
                

                else{
                    System.out.println("Comando desconhecido!");
                }
            }

            else{
                System.out.println("Comando desconhecido!");
            }
        }

    }

    public class Programas {
        public Word[] progMinimo = new Word[] {
                //       OPCODE      R1  R2  P         :: VEJA AS COLUNAS VERMELHAS DA TABELA DE DEFINICAO DE OPERACOES
                //                                     :: -1 SIGNIFICA QUE O PARAMETRO NAO EXISTE PARA A OPERACAO DEFINIDA
                new Word(Opcode.LDI, 0, -1, 999),
                new Word(Opcode.STD, 0, -1, 10),
                new Word(Opcode.STD, 0, -1, 11),
                new Word(Opcode.STD, 0, -1, 12),
                new Word(Opcode.STD, 0, -1, 13),
                new Word(Opcode.STD, 0, -1, 14),
                new Word(Opcode.STOP, -1, -1, -1) };

        public Word[] fibonacci10 = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
                new Word(Opcode.LDI, 1, -1, 0),  //0 coloca 0 no reg 1
                new Word(Opcode.STD, 1, -1, 20),    // 20 posicao de memoria onde inicia a serie de fibonacci gerada //1, ou seja coloca valor de reg 1 (0) na posicao 20
                new Word(Opcode.LDI, 2, -1, 1), //2 coloca 1 no reg 2
                new Word(Opcode.STD, 2, -1, 21), //3 na posição 21 coloca o que está em no reg 2, ou seja coloca 1 na posicao 21
                new Word(Opcode.LDI, 0, -1, 22), //4 coloca 22 no reg 0
                new Word(Opcode.LDI, 6, -1, 6), //5 coloca 6 no reg 6 - linha do inicio do loop
                new Word(Opcode.LDD, 7, -1, 17), //6 coloca 17 no reg 7. É o contador. será a posição one começam os dados, ou seja 20 + a quantidade de números fibonacci que queremos
                new Word(Opcode.LDI, 3, -1, 0), //7 coloca 0 no reg 3
                new Word(Opcode.ADD, 3, 1, -1), //8
                new Word(Opcode.LDI, 1, -1, 0), //9
                new Word(Opcode.ADD, 1, 2, -1), //10 add reg 1 + reg 2
                new Word(Opcode.ADD, 2, 3, -1), //11 add reg 2 + reg 3
                new Word(Opcode.STX, 0, 2, -1), //12 coloca o que está em reg 2 (1) na posição  memória do reg 0 (22), ou seja coloca 1 na pos 22
                new Word(Opcode.ADDI, 0, -1, 1), //13 add 1 no reg 0, ou seja reg fica com 23. Isso serve para mudar a posição da memória onde virá o próximo numero fbonacci
                new Word(Opcode.SUB, 7, 0, -1), //14 reg 7 = reg 7 - o que esta no reg 0, ou seja 30 menos 23 e coloca em r7. Isso é o contador regressivo que fica em r7. se for 0, pára
                new Word(Opcode.JMPIG, 6, 7, -1), //15 se r7 maior que 0 então pc recebe 6, else pc = pc + 1
                new Word(Opcode.STOP, -1, -1, -1),   // POS 16
                new Word(Opcode.DATA, -1, -1, 31), //17 numeros de fibonacci a serem calculados menos 20
                new Word(Opcode.DATA, -1, -1, -1), //18 números de fibonacci a serem calculados
                new Word(Opcode.DATA, -1, -1, -1), //19
                new Word(Opcode.DATA, -1, -1, -1),   // POS 20
                new Word(Opcode.DATA, -1, -1, -1), //21
                new Word(Opcode.DATA, -1, -1, -1), //22
                new Word(Opcode.DATA, -1, -1, -1), //23
                new Word(Opcode.DATA, -1, -1, -1), //24
                new Word(Opcode.DATA, -1, -1, -1), //25
                new Word(Opcode.DATA, -1, -1, -1), //26
                new Word(Opcode.DATA, -1, -1, -1), //27
                new Word(Opcode.DATA, -1, -1, -1), //28
                new Word(Opcode.DATA, -1, -1, -1),  // ate aqui - serie de fibonacci ficara armazenada //29
                new Word(Opcode.DATA, -1, -1, -1)
        };

        // programa que lê o número na posição 21 da memória:
        // - se número < 0: coloca -1 no início da posição de memória para saída, que é 23;
        // - se número > 0: este é o número de valores da sequencia de fibonacci a serem escritos
        // Lembrando que mais 20 números gerará overflow em nosso sistema
        public Word[] fibonacci2 = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
                new Word(Opcode.LDD, 4, -1, 22),    // 0- onde 22 é a posição da memória onde esta a quantidade de números Fibonacci a serem calculados

                // testa se número é menor que 0, e se for manda para final do programa
                new Word(Opcode.JMPILM, -1, 4, 23), // 1- pula para a linha amrazenada em [23], que é a linha de final do programa, se r4<0

                new Word(Opcode.ADDI, 4, -1, 24),   // 2- onde 24 é a primeira posição da memória com dados da fibonacci
                new Word(Opcode.STD, 4, -1, 21),    // 3- armazena o contador na posição 21 da memória

                // armazena valores iniciais da Fibonacci
                new Word(Opcode.LDI, 1, -1, 0),     // 4- coloca 0 no reg 1
                new Word(Opcode.STD, 1, -1, 24),    // 5- 23 posicao de memoria onde inicia a serie de fibonacci gerada, ou seja coloca valor de reg 1 (0) na posicao 23
                new Word(Opcode.LDI, 2, -1, 1),     // 6- coloca 1 no reg 2
                new Word(Opcode.STD, 2, -1, 25),    // 7- na posição 24 coloca o que está em no reg 2, ou seja coloca 1 na posicao 24
                new Word(Opcode.LDI, 0, -1, 26),    // 8- coloca 25 no reg 0

                // início do loop
                new Word(Opcode.LDI, 6, -1, 10),    // 9- coloca 9 no reg 6, onde 9 é a linha do início do loop
                new Word(Opcode.LDD, 7, -1, 21),    // 10- coloca 20 no reg 7. É a posição do o contador.
                new Word(Opcode.LDI, 3, -1, 0),     // 11- coloca 0 no reg 3
                new Word(Opcode.ADD, 3, 1, -1),     // 12-
                new Word(Opcode.LDI, 1, -1, 0),     // 13-
                new Word(Opcode.ADD, 1, 2, -1),     // 14- 0 add reg 1 + reg 2
                new Word(Opcode.ADD, 2, 3, -1),     // 15- 1 add reg 2 + reg 3
                new Word(Opcode.STX, 0, 2, -1),     // 16- coloca o que está em reg 2 (1) na posição  memória do reg 0 (22), ou seja coloca 1 na pos 22
                new Word(Opcode.ADDI, 0, -1, 1),    // 17- add 1 no reg 0, ou seja reg fica com 23. Isso serve para mudar a posição da memória onde virá o próximo numero fbonacci
                new Word(Opcode.SUB, 7, 0, -1),     // 18- reg 7 = reg 7 - o que esta no reg 0, ou seja 30 menos 23 e coloca em r7. Isso é o contador regressivo que fica em r7. se for 0, pára
                new Word(Opcode.JMPIG, 6, 7, -1),   // 19- se r7 maior que 0 então pc recebe 6, else pc = pc + 1
                new Word(Opcode.STOP, -1, -1, -1),  // 20- fim

                // memória
                new Word(Opcode.DATA, -1, -1, -1),  // 21- posição do contador
                new Word(Opcode.DATA, -1, -1, 8),   // 22- números Fibonacci a serem calculados
                new Word(Opcode.DATA, -1, -1, 20),  // 23- linha do final do programa
                new Word(Opcode.DATA, -1, -1, -1),  // 24- início do armazenamento da sequência Fibonacci
                new Word(Opcode.DATA, -1, -1, -1),  // 25-
                new Word(Opcode.DATA, -1, -1, -1),  // 26-
                new Word(Opcode.DATA, -1, -1, -1),  // 27-
                new Word(Opcode.DATA, -1, -1, -1),  // 28-
                new Word(Opcode.DATA, -1, -1, -1),  //...
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1)
        };

        // Dado um inteiro em na posição X da memória,
        // se for negativo armazena -1 na saída; se for positivo responde o fatorial do número na saída
        public Word[] fatorial2 = new Word[] { 	 // este fatorial so aceita valores positivos.   nao pode ser zero
                new Word(Opcode.DATA, -1, -1, 1),   // 0- número a ser calculado o fatorial
                new Word(Opcode.DATA, -1, -1, 12),  // 1- armazena o final do programa
                new Word(Opcode.LDD, 0, -1, 0),     // 2- coloca em reg 0 o valor da memória na posição 0
                new Word(Opcode.LDI, 1, -1, -1),    // 3- deixa reg 1 com -1 por padrão

                // testa se número é menor que 0, e se for manda para final do programa
                new Word(Opcode.JMPILM, -1, 0, 1),  // 4- pula para a linha amrazenada em [1], que é a linha de final do programa, se r0<0

                new Word(Opcode.LDI, 1, -1, 1),      // 5   	r1 é 1 para multiplicar (por r0)
                new Word(Opcode.LDI, 6, -1, 1),      // 6   	r6 é 1 para ser o decremento
                new Word(Opcode.LDI, 7, -1, 12),     // 7   	r7 tem posicao de stop do programa

                // início do loop
                new Word(Opcode.JMPIE, 7, 0, 0),     // 8   	se r0=0 pula para r7(=12)
                new Word(Opcode.MULT, 1, 0, -1),     // 9   	r1 = r1 * r0
                new Word(Opcode.SUB, 0, 6, -1),      // 10   	decrementa r0 1
                new Word(Opcode.JMP, -1, -1, 8),     // 11   	vai p posicao 8, que é o início do loop

                new Word(Opcode.STD, 1, -1, 14),      // 12   	coloca valor de r1 na posição 14
                new Word(Opcode.STOP, -1, -1, -1),    // 13   	stop
                new Word(Opcode.DATA, -1, -1, -1) };  // 14   ao final o valor do fatorial estará na posição 10 da memória


        public Word[] fatorial = new Word[] { 	 // este fatorial so aceita valores positivos.   nao pode ser zero
                // linha   coment
                new Word(Opcode.LDI, 0, -1, 6),      // 0   	r0 é valor a calcular fatorial
                new Word(Opcode.LDI, 1, -1, 1),      // 1   	r1 é 1 para multiplicar (por r0)
                new Word(Opcode.LDI, 6, -1, 1),      // 2   	r6 é 1 para ser o decremento
                new Word(Opcode.LDI, 7, -1, 8),      // 3   	r7 tem posicao de stop do programa = 8
                new Word(Opcode.JMPIE, 7, 0, 0),     // 4   	se r0=0 pula para r7(=8)
                new Word(Opcode.MULT, 1, 0, -1),     // 5   	r1 = r1 * r0
                new Word(Opcode.SUB, 0, 6, -1),      // 6   	decrementa r0 1
                new Word(Opcode.JMP, -1, -1, 4),     // 7   	vai p posicao 4
                new Word(Opcode.STD, 1, -1, 10),     // 8   	coloca valor de r1 na posição 10
                new Word(Opcode.STOP, -1, -1, -1),    // 9   	stop
                new Word(Opcode.DATA, -1, -1, -1) };  // 10   ao final o valor do fatorial estará na posição 10 da memória

        public Word[] invalidAddressTest = new Word []{
                new Word(Opcode.LDD, 0, -1, 1025),
                new Word(Opcode.STOP, -1, -1, -1)
        };

        public Word[] overflowTest = new Word []{
                new Word(Opcode.LDI, 0, -1, 80800),
                new Word(Opcode.LDI, 1, -1, 80800),
                new Word(Opcode.MULT, 0, 1, -1),
                new Word(Opcode.STOP, -1, -1, -1)
        };

        public Word[] bubbleSort = new Word[]{
                // Posições dos loops
                new Word(Opcode.DATA, -1, -1, 41),  // 0- jump do primeiro loop gm ou em
                new Word(Opcode.DATA, -1, -1, 9),   // 1- jump do segundo loop gm ou em
                new Word(Opcode.DATA, -1, -1, 34),  // 2- jump do terceiro loop lm

                // não usadas, mas mantidas devido a dificuldade de refatorar esses códigos
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),

                new Word(Opcode.LDD, 1, -1, 43), // 6- reg 1 vai guardar o tamanho do vetor para comparações
                new Word(Opcode.LDI, 2, -1, 0),  // 7- apenas inicializa vetor 2 com 0
                new Word(Opcode.LDI, 3, -1, 0),  // 8- apenas inicializa vetor 3 com 0

                // início loop externo
                new Word(Opcode.LDI, 5, -1, 0),     // linha 9
                new Word(Opcode.ADD, 5, 2, -1),
                new Word(Opcode.SUB, 5, 1, -1),
                new Word(Opcode.JMPIGM, -1, 5, 0),  // linha 12 - pula pra linha 41 que é o fim (armazenado na memória [0])
                new Word(Opcode.JMPIEM, -1, 5, 0),
                new Word(Opcode.ADDI, 2, -1, 1),
                new Word(Opcode.LDI, 3, -1, 0),     // 15-

                // início loop interno
                new Word(Opcode.LDI, 5, -1, 0),     // 16-
                new Word(Opcode.ADD, 5, 3, -1),
                new Word(Opcode.ADDI, 5, -1, 1),

                // Verifica se chegou ao final do vetor. Se sim,reinicia comparações.
                new Word(Opcode.SUB, 5, 1, -1),     // 19-
                new Word(Opcode.JMPIEM, -1, 5, 1),  // 20- Pula para linha 9 (armazenado na memória [1]). Loop externo
                new Word(Opcode.JMPIGM, -1, 5, 1),
                // fim loop externo

                // Coloca nos registradores 4 e 5 dois números adjacentes do vetor
                new Word(Opcode.LDI, 4, -1, 44),    // 21- coloca a posição da memória de início do vetor [44] no reg 4
                new Word(Opcode.ADD, 4, 3, -1),     // 22-
                new Word(Opcode.LDI, 5, -1, 1),     // 23-
                new Word(Opcode.ADD, 5, 4, -1),     // 24-
                new Word(Opcode.LDX, 4, 4, -1),
                new Word(Opcode.LDX, 5, 5, -1),

                new Word(Opcode.ADDI, 3, -1, 1),    // 27- é o incremento da posição do vetor

                // Testa se o segundo número é menor que o primeiro
                new Word(Opcode.LDI, 6, -1, 0),
                new Word(Opcode.ADD, 6, 5, -1),
                new Word(Opcode.SUB, 6, 4, -1),
                new Word(Opcode.JMPILM, -1, 6, 2),  // 32- pula pra linha 34 se o segundo número é menor que o primeiro (amazenado na memória [2])
                new Word(Opcode.JMP, -1, -1, 16),   // 33- se não for volta pro início do loop interno
                // fim loop interno

                // Faz swap de dois números, se o segundo for menor que o primeiro
                new Word(Opcode.SWAP, 5, 4, -1),    // 34-
                new Word(Opcode.LDI, 6, -1, 43),    // 35-
                new Word(Opcode.ADD, 6, 3, -1),
                new Word(Opcode.STX, 6, 4, -1),
                new Word(Opcode.ADDI, 6, -1, 1),
                new Word(Opcode.STX, 6, 5, -1),
                new Word(Opcode.JMP, -1, -1, 16),   // 40-

                new Word(Opcode.STOP, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1),  // 42- não usada
                new Word(Opcode.DATA, -1, -1, 6),   // 43- tamanho do vetor
                new Word(Opcode.DATA, -1, -1, 12),  // 44- dados do vetor a partir daqui até o final
                new Word(Opcode.DATA, -1, -1, 7),
                new Word(Opcode.DATA, -1, -1, 9),
                new Word(Opcode.DATA, -1, -1, 1),
                new Word(Opcode.DATA, -1, -1, 4),
                new Word(Opcode.DATA, -1, -1, 3)
        };

        public Word[] trapTestOutput = new Word[]{
                new Word(Opcode.LDI, 1, -1, 44),    //coloca 44 no reg 1. Esse será o valor mostrado no output
                new Word(Opcode.STD, 1, -1, 6),     // coloca o valor de reg1 na posição 6 da memória
                new Word(Opcode.LDI, 8, -1, 2),     // coloca 2 em reg 8 para criar um trap de out
                new Word(Opcode.LDI, 9,-1,6),       // coloca 6 no reg 9, ou seja a posição onde será feita a leitura
                new Word(Opcode.TRAP,-1,-1,-1),     // faz o output da posição 6
                new Word(Opcode.STOP, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1)
        };

        public Word[] trapTestInput = new Word[]{
                new Word(Opcode.LDI, 8, -1, 1),     // coloca 2 em reg 8 para criar um trap de input
                new Word(Opcode.LDI, 9,-1,4),       // coloca 4 no reg 9, ou seja a posição onde será feita a escrita
                new Word(Opcode.TRAP,-1,-1,-1),     // faz o input e armazena na posição 4
                new Word(Opcode.STOP, -1, -1, -1),
                new Word(Opcode.DATA, -1, -1, -1)   // valor será armazenado aqui
        };

        public Word[] invalidRegisterTest = new Word[]{
                new Word(Opcode.LDD, 11, -1, 1),
                new Word(Opcode.STOP, -1, -1, -1)
        };

        public Word[] fibonacciComOutput = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
                new Word(Opcode.LDI, 1, -1, 0),  //0 coloca 0 no reg 1
                new Word(Opcode.STD, 1, -1, 23),    // 20 posicao de memoria onde inicia a serie de fibonacci gerada //1, ou seja coloca valor de reg 1 (0) na posicao 20
                new Word(Opcode.LDI, 2, -1, 1), //2 coloca 1 no reg 2
                new Word(Opcode.STD, 2, -1, 24), //3 na posição 21 coloca o que está em no reg 2, ou seja coloca 1 na posicao 21
                new Word(Opcode.LDI, 0, -1, 25), //4 coloca 22 no reg 0
                new Word(Opcode.LDI, 6, -1, 6), //5 coloca 6 no reg 6 - linha do inicio do loop
                new Word(Opcode.LDI, 7, -1, 34), //6 coloca 34 no reg 7. É o contador. será a posição one começam os dados, ou seja 23 + a quantidade de números fibonacci que queremos
                new Word(Opcode.LDI, 3, -1, 0), //7 coloca 0 no reg 3
                new Word(Opcode.ADD, 3, 1, -1), //8
                new Word(Opcode.LDI, 1, -1, 0), //9
                new Word(Opcode.ADD, 1, 2, -1), //10 add reg 1 + reg 2
                new Word(Opcode.ADD, 2, 3, -1), //11 add reg 2 + reg 3
                new Word(Opcode.STX, 0, 2, -1), //12 coloca o que está em reg 2 (1) na posição  memória do reg 0 (22), ou seja coloca 1 na pos 22
                new Word(Opcode.ADDI, 0, -1, 1), //13 add 1 no reg 0, ou seja reg fica com 23. Isso serve para mudar a posição da memória onde virá o próximo numero fbonacci
                new Word(Opcode.SUB, 7, 0, -1), //14 reg 7 = reg 7 - o que esta no reg 0, ou seja 30 menos 23 e coloca em r7. Isso é o contador regressivo que fica em r7. se for 0, pára
                new Word(Opcode.JMPIG, 6, 7, -1), //15 se r7 maior que 0 então pc recebe 6, else pc = pc + 1

                // output
                new Word(Opcode.LDI, 8, -1, 2),     // coloca 2 em reg 8 para criar um trap de out
                new Word(Opcode.LDI, 9,-1,33),      // coloca 6 no reg 9, ou seja a posição onde será feita a leitura
                new Word(Opcode.TRAP,-1,-1,-1),     // faz o output da posição 10

                // memória
                new Word(Opcode.STOP, -1, -1, -1),   // POS 16
                new Word(Opcode.DATA, -1, -1, 31), //17 numeros de fibonacci a serem calculados menos 20
                new Word(Opcode.DATA, -1, -1, -1), //18
                new Word(Opcode.DATA, -1, -1, -1), //19
                new Word(Opcode.DATA, -1, -1, -1),   // POS 20
                new Word(Opcode.DATA, -1, -1, -1), //21
                new Word(Opcode.DATA, -1, -1, -1), //22
                new Word(Opcode.DATA, -1, -1, -1), //23
                new Word(Opcode.DATA, -1, -1, -1), //24
                new Word(Opcode.DATA, -1, -1, -1), //25
                new Word(Opcode.DATA, -1, -1, -1), //26
                new Word(Opcode.DATA, -1, -1, -1), //27
                new Word(Opcode.DATA, -1, -1, -1), //28
                new Word(Opcode.DATA, -1, -1, -1),  // ate aqui - serie de fibonacci ficara armazenada //29
                new Word(Opcode.DATA, -1, -1, -1)
        };

        // Usuário faz input de um inteiro que é armazenado na posição 3 da memória,
        // se for negativo armazena -1 na saída [17]; se for positivo responde o fatorial do número na saída[17]
        public Word[] fatorialComInput = new Word[] {
                // input
                new Word(Opcode.LDI, 8, -1, 1),    // 0- coloca 1 em reg 8 para criar um trap de input
                new Word(Opcode.LDI, 9,-1,3),      // 1- coloca 3 no reg 9, ou seja a posição onde será feita a escrita do input
                new Word(Opcode.TRAP,-1,-1,-1),    // 2- faz o input

                new Word(Opcode.DATA, -1, -1, -1),   // 3- número a ser calculado o fatorial
                new Word(Opcode.DATA, -1, -1, 15),  // 4- armazena o final do programa
                new Word(Opcode.LDD, 0, -1, 3),     // 5- coloca em reg 0 o valor da memória na posição 3, que é o número a ser calculado
                new Word(Opcode.LDI, 1, -1, -1),    // 6- deixa reg 1 com -1 por padrão

                // testa se número é menor que 0, e se for manda para final do programa
                new Word(Opcode.JMPILM, -1, 0, 1),  // 7- pula para a linha amrazenada em [1], que é a linha de final do programa, se r0<0

                new Word(Opcode.LDI, 1, -1, 1),      // 8   	r1 é 1 para multiplicar (por r0)
                new Word(Opcode.LDI, 6, -1, 1),      // 9   	r6 é 1 para ser o decremento
                new Word(Opcode.LDI, 7, -1, 15),     // 10   	r7 tem posicao de stop do programa

                // início do loop
                new Word(Opcode.JMPIE, 7, 0, 0),     // 11   	se r0=0 pula para r7(=15)
                new Word(Opcode.MULT, 1, 0, -1),     // 12   	r1 = r1 * r0
                new Word(Opcode.SUB, 0, 6, -1),      // 13   	decrementa r0 1
                new Word(Opcode.JMP, -1, -1, 11),     // 14   	vai p posicao 11, que é o início do loop

                new Word(Opcode.STD, 1, -1, 17),      // 15   	coloca valor de r1 na posição 17
                new Word(Opcode.STOP, -1, -1, -1),    // 16  	stop
                new Word(Opcode.DATA, -1, -1, -1) };  // 17   ao final o valor do fatorial estará na posição 17 da memória

    }
}


