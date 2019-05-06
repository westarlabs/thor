#!/usr/bin/env bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
COMPOSE_FILE="$SCRIPTPATH/docker/docker-compose.yml"
export NETWORK="simnet"
init(){
    echo "===============init env================="
    docker-compose -f $COMPOSE_FILE pull btcd lnd
}

startlnd(){
    echo -e "===============start lnd for $1==================="
    docker-compose -f $COMPOSE_FILE run -d --name $1 lnd_btc
    sleep 10
}



new_address(){
    docker exec -i -t $1 lncli --network=simnet newaddress np2wkh|grep address | awk -F "\"" '{print $4}'
}


start_btcd(){
    address=$(new_address $1)
    echo -e "=======start btcd with mining address: $adderss======="
    MINING_ADDRESS=$address docker-compose -f $COMPOSE_FILE up -d btcd
    docker-compose -f $COMPOSE_FILE run btcctl generate 400
}

clean(){
    echo "==============clean env=================="
    docker rm alice -f
    docker rm bob -f
    docker-compose -f $COMPOSE_FILE stop
    docker-compose -f $COMPOSE_FILE rm -f
}
init
clean
startlnd alice
start_btcd alice
startlnd bob

