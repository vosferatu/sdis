In the main directory run:

sh scripts/compile.sh

to compile and launch RMI registry.



In the main directory run:

sh scripts/peer.sh 1 1 peer1

to launch a peer in the version 1, id 1 and RMI access point "peer1"



In the main directory run:

sh scripts/client.sh peer1 BACKUP /home/me/Desktop/myfile.txt 3

to launch the BACKUP protocol to the /home/me/Desktop/myfile.txt file
with the replication degree 3, connecting to peer1

This script functions like the project description:

sh scripts/client.sh peer1 <access_point> <sub_protocol> [<opnd_1> <opnd_2>]


"


<peer_ap>
    Is the peer's access point. RMI implementation
<operation>
    Is the operation the peer of the backup service must execute.
    It can be either the triggering of the subprotocol to test,
    or the retrieval of the peer's internal state.
    In the first case it must be one of: BACKUP, RESTORE, DELETE, RECLAIM.
    In the case of enhancements, you must append the substring ENH at the end
    of the respecive subprotocol, e.g. BACKUPENH.
    To retrieve the internal state, the value of this argument must be STATE
<opnd_1>
    Is either the path name of the file to backup/restore/delete, for the respective 3 subprotocols,
    or, in the case of RECLAIM the maximum amount of disk space (in KByte) that the service can use to store the chunks.
    In the latter case, the peer should execute the RECLAIM protocol, upon deletion of any chunk.
    The STATE operation takes no operands.
<opnd_2>
    This operand is an integer that specifies the desired replication degree and applies only to the backup protocol (or its enhancement) "
