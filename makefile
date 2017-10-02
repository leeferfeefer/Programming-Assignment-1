# Dan Fincher
# Programming Assignment 1 makefile
# Platform: OSX


# Define compiler and flag variables
JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

# Macro defining classes to compile
CLASSES = \
		User.java \
		UserData.java \
		ServerTcp.java \
		TCPprotocol.java \
		RemoteBankTcp.java \
		ServerUdp.java \
		UDPprotocol.java \
		RemoteBankUdp.java 

# Default make target entry
default: classes

# Use macro to define .java = .class suffix
classes: $(CLASSES:.java=.class)

# Remove class files
clean: 
		$(RM) *.class