import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    private int a_AckNum;
    private int b_AckNum;

    private int a_SeqNum;
    private int b_SeqNum;

    private ArrayList<Message> messageList = new ArrayList<Message>(50);
    private int currentIndex = 0;
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = 2*winsize;
	RxmtInterval = delay;
    }

    // Check to see if the packet is corrupted
    public boolean isCorruption(Packet message){
        int messageCheckSum = 0;
        int calculatedCheckSum = 0;

        messageCheckSum = message.getChecksum();
        calculatedCheckSum = computeCheckSum(message.getPayload(),
                                             message.getAcknum(),
                                             message.getSeqnum());

        if(messageCheckSum == calculatedCheckSum){
            return false;
        }
        else{
            return true;
        }
    }

    // Calculate the checkSum
    public int computeCheckSum(String data, int ACKNUM, int SEQNUM){
       int checkSum = 0;
       for(int i=0; i < data.length(); i++){
           checkSum += (int) data.charAt(i);
       }

       checkSum += ACKNUM + SEQNUM;
       return checkSum;
    }

    // Recompute the Ack Number if the Ack Number has been corrupted
    public void computeAckNum(String data, int SEQNUM, int checkSum){

    }

    // Message to Packet
    public Packet messageToPacket(Message message, int seqNum, int ackNum){
        Packet transition = new Packet(seqNum,
                                       ackNum,
                                       computeCheckSum(message.getData(), ackNum, seqNum),
                                       message.getData());
        return transition;
    }

    // Transition the Ack numbers
    public void setAckNum(int entity){
       if( entity == 0){
           if( this.a_AckNum == 0){
               this.a_AckNum = 1;
           }
           else{
               this.a_AckNum = 0;
           }
       }
       else if(entity == 1){
           if( this.b_AckNum == 0){
                this.b_AckNum = 1;
           }
           else{
                this.b_AckNum = 0;
           }
        }
    } 

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        System.out.println("\n\n\n----- In aOutput -----\n\n\n");
      
        Packet packet = this.messageToPacket(message, this.a_SeqNum, this.a_AckNum);
        
        // Drop any packets when the buffer is full
        if(this.messageList.size() < 50){
            this.messageList.add(message);
        }
        else{
            System.out.println("Buffer is full!");
            System.exit(0);
        }

        if(this.messageList.size() == 1){
            super.toLayer3(0, packet);
            super.startTimer(0, this.RxmtInterval);
        }
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        System.out.println("\n\n\n----- In aInput -----\n\n\n");

        System.out.println("A's AckNum is " + Integer.toString(this.a_AckNum));
        System.out.println("B's AckNum is " + Integer.toString(packet.getAcknum()));
        if(packet.getAcknum() == this.a_AckNum){
            this.stopTimer(0);
            // Delete the guarenteed delivered Message
            this.messageList.remove(this.currentIndex);
            // Change the Ack Numbers
            this.setAckNum(0);
            this.a_SeqNum += 1;
            
     
            // If there are messages in the buffer
            if(this.messageList.size() > 0){
                Message nextMessage = messageList.get(this.currentIndex);
                super.toLayer3(0, this.messageToPacket(nextMessage, this.a_SeqNum, this.a_AckNum));
                super.startTimer(0, this.RxmtInterval);
            }
        }
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        System.out.println("\n\n\n----- In aTimerInterrupt -----\n\n\n");
  
        Message resend = messageList.get(this.currentIndex);
        Packet resendPacket = this.messageToPacket(resend, this.a_SeqNum, this.a_AckNum);

        super.toLayer3(0, resendPacket);
        super.startTimer(0, this.RxmtInterval);      
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        this.a_AckNum = 0;
        this.a_SeqNum = 0;
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        System.out.println("\n\n\n----- In bInput -----\n\n\n");
      
        // If B's sequence number is higher than the packet's
        // Note: Be best to check in Corruption if statement
        if(this.b_SeqNum > packet.getSeqnum()){
            System.out.println("I will send an ACK back to user");
            this.setAckNum(1);
            Packet reply = new Packet(this.b_SeqNum, this.b_AckNum, packet.getChecksum());
            this.setAckNum(1);
            super.toLayer3(1, reply);
        } 
        // If packet is not corrupted
        else if(!(isCorruption(packet))){
            // Send the payload to layer 5 B
            super.toLayer5(packet.getPayload());

            this.b_SeqNum += 1;

            // Send a reponse back to A
            Packet reply = new Packet(this.b_SeqNum, this.b_AckNum, packet.getChecksum());
            this.setAckNum(1);
            super.toLayer3(1, reply);
        }
        else{
            System.out.println("Corrupted Packet: " + packet.getPayload());
        }
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        this.b_AckNum = 0;
        this.b_SeqNum = 0;
    }

    // Use to print final statistics
    protected void Simulation_done()
    {

    }	
}
