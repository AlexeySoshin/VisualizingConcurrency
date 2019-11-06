(function () {
    const url = "ws://localhost:8080/ws";
    const client = new WebSocket(url)

    const graph = cytoscape({
        container: document.getElementById('main'),

        boxSelectionEnabled: false,
        autounselectify: true,

        style: cytoscape.stylesheet()
            .selector('node')
            .style({
                'content': 'data(name)',
                'background-color': '#001f3f',
            })
            .selector('edge')
            .style({
                'curve-style': (window.location.search.endsWith("join")) ? 'taxi' : 'bezier',
                "taxi-direction": "downward",
                "taxi-turn": 20,
                "taxi-turn-min-distance": 10,
                'target-arrow-shape': 'triangle',
                'width': 4,
                'line-color': '#001f3f',
                'target-arrow-color': '#001f3f',
                'z-index': 1
            })
            .selector('.highlighted')
            .style({
                'background-color': '#2ECC40',
                'line-color': '#2ECC40',
                'target-arrow-color': '#2ECC40',
                'transition-property': 'background-color, line-color, target-arrow-color',
                'transition-duration': '0.1s',
                'z-index': 100
            })
            .selector('#producer')
            .style({
                'background-color': '#2ECC40'
            })
            .selector("#collector")
            .style({
                'text-valign': "bottom"
            }),

        elements: {
            nodes: [
                {data: {id: 'producer', name: 'HTML Fetcher'}},
                {data: {id: 'collector', name: 'Image Saver'}}
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

    graph.$("#producer").qtip({
        content: function(){ return `<img src = '/scrap.jpg' />`},
        position: {
            my: 'top center',
            at: 'bottom center'
        },
        show: {
            solo: true
        }
    });

    graph.$("#collector").qtip({
        content: function(){ return `<img src = '/save.jpg' />`},
        position: {
            my: 'top center',
            at: 'bottom center'
        },
        show: {
            solo: true
        }
    });

    client.addEventListener('message', (event) => {

        let message = event.data.split(":")
        let type = message[0]
        let id = message[1]
        let payload = message[2]

        switch (type) {
            case "producer":
                handleProducer(id, payload)
                break;
            case "consumer":
                handleConsumer(id, payload)
                break;
            case "collector":
                handleCollector(id, payload)
                break;
        }

    });



    const producers = []
    const consumers = []

    function handleProducer(id, payload) {
        graph.$("#producer").data('name', 'HTML Fetcher: ' + payload)
    }

    function handleConsumer(id, payload) {
        if (graph.$(`#p${id}`).length === 0) {
            producers.push(id)

            graph.add([
                {group: 'nodes', data: {id: `consumer${id}`, name: `ImageFetcher ${(parseInt(id)+1)}`}},
                {group: 'edges', data: {id: `p${id}`,
                        source: 'producer',
                        target: `consumer${id}`}}
            ])

            graph.layout({
                name: 'breadthfirst',
                directed: true,
                roots: '#producer',
                padding: 10
            }).run()

            graph.$(`#consumer${id}`).qtip({
                    content: function(){ return `<img src = '/downloader.jpg' />`},
                    position: {
                        my: 'top center',
                        at: 'bottom center'
                    },
                    show: {
                        solo: true
                    }
                });
        }

        for (i in producers) {
            graph.$(`#p${i}`).removeClass("highlighted")
        //    graph.$(`#consumer${i}`).removeClass("highlighted")
        }

        graph.$(`#p${id}`).addClass("highlighted")
     //   graph.$(`#consumer${id}`).addClass("highlighted")
    }

    function handleCollector(id, payload) {
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

        for (let i in consumers) {
            graph.$(`#c${i}`).removeClass("highlighted")
            graph.$(`#consumer${i}`).removeClass("highlighted")
        }

        graph.$(`#collector`).data('name', 'ImageSaver: ' + payload)

        graph.$(`#c${id}`).addClass("highlighted")
        graph.$(`#consumer${id}`).addClass("highlighted")
    }

})()
