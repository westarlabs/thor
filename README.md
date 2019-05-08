# Thor - The most advanced bitcoin layer 2 Start Contract platform and LightningNetwork game framework

## About

Thor is the  most advanced bitcoin layer 2 Start Contract platform which is based on  LightningNetwork. At the same time ,part of this project is lightningnetwork game framework. Project is under heavy development, not production ready.

Inspiration of the name "Thor" comes from the Germanic mythology which is the god of lightning.

## Overview


## How Thor works
Assume Bob and Alice want to play Gomoku throw Thor, the workflow is

1. Authorization
   Both of the game client Alice and Bob request arbitrate service to generate a `nonce with signature`, then sending back that `nonce with signature` after verifying the nonce with the `public key of arbitrate`.

2. Create game room
   Alice request arbitrate service to create a game room then waiting for Bob to join by giving the `room id` to him.

3. Funding btc for game
   1. Arbitrate service generated a pair of `(r, rhash)` for both Alice and Bob and just giving the `rhash` to them.
   2. Game clients connect to the LN node, exchange the invoice which generated with the rhash by `INVOICE_DATA`, then pay for the invoice.
   3. Both game client confirm the state of their invoice is `ACCEPTED`.

4. Paly game
   1. Arbitrate service will broadcast a `GAME_BEGIN` message to the room after the `READY` message beening sent from all game clients in the same room.
   2. Game playing will be proceed through runing the `witness data` in vm which generated by both game clients and arbitrate service.
   
5. Finish game and challenging
   1. If someone surrender to another, just finish peacefully.
   2. If client is timeout, the arbitrate notify another client to request a challenge with the proof data and wait a duration.
   3. The arbitrate serivice will replay the game by running the proof data in vm after a challenge recived and find the winner.
   4. The winner will get the "secrect" `r` of the loser to unlock the funding btc in lightning network after the broadcast of arbitrate. If tie happen,
	  both client get the `r`. After all, game end.
