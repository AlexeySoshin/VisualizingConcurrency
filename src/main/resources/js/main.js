(function () {
    var url = "ws://localhost:8080/ws";
    var client = new WebSocket(url)


    var graph = cytoscape({
        container: document.getElementById('main'),

        boxSelectionEnabled: false,
        autounselectify: true,

        style: cytoscape.stylesheet()
            .selector('node')
            .style({
                'content': 'data(name)'
            })
            .selector('edge')
            .style({
                'curve-style': 'bezier',
                'target-arrow-shape': 'triangle',
                'width': 4,
                'line-color': '#ddd',
                'target-arrow-color': '#ddd'
            })
            .selector('.highlighted')
            .style({
                'background-color': '#61bffc',
                'line-color': '#61bffc',
                'target-arrow-color': '#61bffc',
                'transition-property': 'background-color, line-color, target-arrow-color',
                'transition-duration': '0.1s'
            })
            .selector('#producer')
            .style({
                'background-color': '#61bffc'
            }),

        elements: {
            nodes: [
                {data: {id: 'producer', name: 'producer'}},
                {data: {id: 'collector', name: 'collector'}}
            ],

            edges: []
        },

        layout: {
            name: 'breadthfirst',
            directed: true,
            roots: '#producer',
            padding: 10
        }
    });

    client.addEventListener('message', (event) => {

        let message = event.data.split(":")
        let type = message[0]
        let id = message[1]
        let payload = message[2]

        switch (type) {
            case "consumer":
                handleConsumer(id, payload)
                break;
            case "collector":
                handleProducer(id, payload)
                break;
        }

    });


    const producers = []
    const consumers = []

    function handleProducer(id, payload) {
        if (graph.$(`#p${id}`).length === 0) {
            producers.push(id)

            graph.add([
                {group: 'nodes', data: {id: `consumer${id}`, name: `consumer${id}`}},
                {group: 'edges', data: {id: `p${id}`, source: 'producer', target: `consumer${id}`}}
            ])

            graph.layout({
                name: 'breadthfirst',
                directed: true,
                roots: '#producer',
                padding: 10
            }).run()
        }

        graph.$(`#collector`).data('name', 'collector: ' + payload)

        for (i in producers) {
            graph.$(`#p${i}`).removeClass("highlighted")
            graph.$(`#consumer${i}`).removeClass("highlighted")
        }

        graph.$(`#p${id}`).addClass("highlighted")
        graph.$(`#consumer${id}`).addClass("highlighted")
    }

    function handleConsumer(id, payload) {
        if (graph.$(`#c${id}`).length === 0) {
            consumers.push(id)

            graph.add([
                {group: 'edges', data: {id: `c${id}`, source: `consumer${id}`, target: `collector`}}
            ])

            graph.layout({
                name: 'breadthfirst',
                directed: true,
                roots: '#producer',
                padding: 10
            }).run()
        }

        for (i in consumers) {
            graph.$(`#c${i}`).removeClass("highlighted")
            graph.$(`#consumer${i}`).removeClass("highlighted")
        }

        graph.$("#producer").data('name', 'producer: ' + payload)

        graph.$(`#c${id}`).addClass("highlighted")
        graph.$(`#consumer${id}`).addClass("highlighted")
    }

})()
