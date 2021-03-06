# Thor - A smart contract and arbitrate oracle service on Lightning Network

## About

Thor is a bitcoin layout 2 implement base on Lightning Network, users can develop
smart contract with WebAssembly on it and we also implement a arbitrate oracle
service to guarantee trustworthy of the applications running on it. Project is
under heavy development, not production ready.

Inspiration of the name "Thor" comes from the Germanic mythology which is the
god of lightning.

## Overview

Since bitcoin network does not have a mature layout 2 smart contract implementation
currently, Thor is a experimental project for it. The smart contracts on Thor are
compiled to WebAssembly, and the arbitrate oracle service is a hash time locked
contract implementation through Lightning Network. Developers can write the smart
contract by implement the interface of the thor contract abi.

## Architecture
```text
+------------------+                   +--------------------+
|  Thor client     |----StatesManage---|  thor Arbitrater   |
+------------------+                   +--------------------+
|                  |----Arbitrate------|                    |
|  Smart contract  |                   |  Smart contract    |
|  Vm (WebAssembly)|   	       	       |  Vm (WebAssembly)  |
+--------+---------+                   +---------+----------+
         |                                       |
         |                                       |
         |                HTLC                   |
     	 +-----------------+---------------------+
                           |
              	        Funding
                           |
                           |
              +------------v--------------+
              |    Lightning Network      |
              +---------------------------+

```
## How Thor works
Assume Bob and Alice want to play Gomoku throw Thor, the workflow is below

1. Authorization
   Both of the game client Alice and Bob request arbitrate service to generate a `nonce with signature`, then sending back that `nonce with their signature` after verifying the nonce with the `public key of arbitrate`, after the arbitrate verified those signatures, the authorization finished.

2. Create game room
   Alice request arbitrate service to create a game room and join it then waiting for Bob to join by giving the `room id` to him.

3. Funding btc for game
   1. Arbitrate service generated a pair of `(r, rhash)` for both Alice and Bob and just giving the `rhash` to them.
   2. Game clients connect to the LN node, exchange the invoice which generated with the rhash by `INVOICE_DATA`, then pay for the invoice.
   3. Both game client confirm the state of their invoice is `ACCEPTED`.

4. Play game
   1. Arbitrate service will broadcast a `GAME_BEGIN` message to the room after the `READY` message beening sent from all game clients in the same room.
   2. Game playing will be proceed through runing the `witness data` in vm which generated by both game clients and arbitrate service.

5. Finish game and challenging
   1. If someone surrender to another, just finish peacefully.
   2. both clients can request a challenge with the proof of data during a challenge period.
   3. The arbitrate serivice will replay the game by running the proof data in vm after a challenge recived and find the winner.
   4. The winner will get the "secrect" `r` of the loser to unlock the funding btc in lightning network after the broadcast of arbitrate. If tie happen,
	  the `r` would not be broadcast, and both clients cancel the invoice payed before. After all, game end.

![image](https://github.com/starcoinorg/thor/blob/master/docs/thor_workflow.png?raw=true)
