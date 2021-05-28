pipeline{
    agent {
        docker { image 'node:9-alpine' },
        label 'worker'
    }
}